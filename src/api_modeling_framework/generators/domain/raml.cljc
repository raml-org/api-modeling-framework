(ns api-modeling-framework.generators.domain.raml
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.generators.domain.shapes-raml-types :as shapes-parser]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.generators.domain.utils :refer [send]]
            [api-modeling-framework.generators.domain.common :as common]
            [clojure.string :as string]
            [cemerick.url :as url]
            [clojure.walk :refer [keywordize-keys]]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) [debug]]))


(defn to-raml-dispatch-fn [model ctx]
  (cond
    (nil? model)                                    model

    (satisfies? domain/DomainPropertySchema model)  domain/DomainPropertySchema

    (and (satisfies? document/Includes model)
         (satisfies? document/Node model))          document/Includes


    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))          domain/APIDocumentation

    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))          domain/EndPoint

    (and (satisfies? domain/Operation model)
         (satisfies? document/Node model))          domain/Operation

    (and (satisfies? domain/PayloadHolder model)
         (satisfies? domain/HeadersHolder model)
         (satisfies? domain/ParametersHolder model)
         (satisfies? document/Node model))          :Request

    (and (satisfies? domain/Response model)
         (satisfies? document/Node model))          domain/Response

    (and (satisfies? domain/Type model)
         (satisfies? document/Node model))          domain/Type

    :else                                           (type model)))

(defmulti to-raml (fn [model ctx] (to-raml-dispatch-fn model ctx)))


(defn with-annotations [model ctx generated]
  (if (map? generated)
    (let [annotations (document/additional-properties model)
          annotations-map (->> annotations
                               (map (fn [annotation]
                                      [(str "(" (document/name annotation) ")") (->  annotation domain/object utils/jsonld->annotation)]))
                               (into {}))]
      (merge generated
             annotations-map))
    generated))

(defn includes? [x]
  (if (and (some? x) (some? (document/extends x)) (= 1 (count (document/extends x))))
    (let [extended (document/extends x)
          included-tag (first (document/find-tag (first extended) document/extend-include-fragment-parsed-tag))]
      (some? included-tag))
    false))

(defn to-raml! [base-element {:keys [fragments expanded-fragments document-generator] :as ctx}]
  (if (includes? base-element) ;; is this node merging something?
    (let [fragment-target (document/target (first (document/extends base-element)))
          fragment (get fragments fragment-target)
          expanded-fragment (if (nil? fragment)
                              (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " fragment-target " for generation")))
                              ;; we first check in the expansion cache
                              (if-let [expanded-fragment (get expanded-fragments fragment-target)]
                                expanded-fragment
                                ;; not in the cache we compute the value
                                (let [expanded-fragment (common/merge-fragment base-element fragment ctx)]
                                  ;; before returning the expanded fragment, we saved it in the cache
                                  (swap! expanded-fragments (fn [acc] (assoc acc fragment-target expanded-fragment)))
                                  expanded-fragment)))]
      (with-annotations (document/encodes fragment) ctx
        expanded-fragment))
    ;; Nothing to merge
    (with-annotations base-element ctx
      (to-raml base-element ctx))))

(defn model->base-uri [model]
  (let [;;scheme (or (domain/scheme model) [])
        host (domain/host model)
        base-path (domain/base-path model)]
    (cond
      (some? host)      (str host base-path)
      (some? base-path) base-path
      :else             nil)))

(defn model->protocols [model]
  (let [schemes (domain/scheme model)]
    (cond
      (and (some? schemes)
           (= 1 (count schemes))) (first schemes)
      (some? schemes)             schemes
      :else                       nil)))

(defn model->media-type [model]
  (let [contents (or (domain/content-type model) [])
        accepts (or (domain/accepts model) [])
        media-types (distinct (concat contents accepts))]
    (cond
      (empty? media-types)      nil
      (nil? media-types)        nil
      (= 1 (count media-types)) (first media-types)
      :else                     media-types)))

(defn find-children-resources
  "Find the children for a particular model using the information stored in the tags"
  [id children]
  (->> children
       (filter (fn [child] (= (-> child
                                 (document/find-tag document/nested-resource-parent-id-tag)
                                 (->> (mapv document/value))
                                 first)

                             id)))))

(defn unparse-parameters [parameters context]
  (if (nil? parameters) nil
      (->> parameters
           (map (fn [parameter]
                  (let [parsed-type (keywordize-keys (shapes-parser/parse-shape (domain/shape parameter) (assoc context :to-raml to-raml!)))
                        ;; @todo should we really keep a source-map to see if we should add this mapping by default?
                        ;;parsed-type (if (= "string" (:type parsed-type))
                        ;;              (dissoc parsed-type :type)
                        ;;              parsed-type)
                        parsed-type (if (and (some? (domain/required parameter))
                                             (= false (domain/required parameter)))
                                      (let [parsed-type (if (map? parsed-type) parsed-type {:type parsed-type})]
                                        (assoc parsed-type :required (domain/required parameter)))
                                      parsed-type)]
                    [(keyword (document/name parameter))
                     parsed-type])))
           (into {}))))

(defn merge-children-resources
  "We merge the children in the current node using the paths RAML style"
  [node children-resources ctx]
  (let [children-node (->> children-resources
                           (mapv (fn [child]
                                   (let [child-path (-> child
                                                        (document/find-tag document/nested-resource-path-parsed-tag)
                                                        first)
                                         ;; the node might come from a OpenAPI model, it will not have path tag
                                         child-path (if (some? child-path)
                                                      (document/value child-path)
                                                      (domain/path child))]
                                     [(keyword (utils/safe-str child-path)) (to-raml! child ctx)]))))
        children-node (into {} children-node)]
    (merge node children-node)))

(defn model->generic-declarations [{:keys [references] :as ctx}]
  (->> references
       (filter (fn [ref] (and (not (common/trait-reference? ref))
                             (not (common/type-reference? ref))
                             (not (common/annotation-reference? ref))
                             (not (satisfies? domain/Type ref)))))
       (map (fn [ref] [(or (:name ref) (:id ref)) (to-raml! ref ctx)]))
       (into {})))

(defmethod to-raml domain/APIDocumentation [model ctx]
  (debug "Generating RAML root node")
  (let [all-resources (domain/endpoints model)
        ctx (assoc ctx :all-resources all-resources)
        children-resources (find-children-resources (document/id model) all-resources)
        ;; this can happen if we are generating from a model that does not generate the tags,
        ;; e.g. OpenAPI, in this case all the children resources are children of the APIDocumentation node
        children-resources (if (empty? children-resources) all-resources children-resources)]
    (-> {:title (document/name model)
         :description (document/description model)
         :version (domain/version model)
         :baseUri (model->base-uri model)
         :baseUriParameters (unparse-parameters (domain/parameters model) ctx)
         :protocols (model->protocols model)
         :annotationTypes (:annotations ctx)
         :mediaType (model->media-type model)
         ;; In our model declared references are not restricted to types, traits
         ;; etc as in RAML, we need to provide a reference for them
         ;; We will use the (declares) annotation for this.
         ;; This can happen for example, when the model comes from an OpenAPI
         ;; document using #definitions containing a random part of the document
         ;; we cannot match with the parts of the document exposed by RAML
         (keyword "(declares)") (model->generic-declarations ctx)
         :types  (common/model->types (assoc ctx :resolve-types true) to-raml!)
         ;; we mark is-trait in the context as a way of signaling the gnenerator
         ;; for method not to display the name
         :traits (common/model->traits (assoc ctx :is-trait true) to-raml!)}
        (merge-children-resources children-resources ctx)
        (->> (with-annotations model ctx)
             (common/with-amf-info model ctx "API"))
        utils/clean-nils)))

(defmethod to-raml domain/EndPoint [model {:keys [all-resources] :as ctx}]
  (debug "Generating resource " (document/id model))
  (let [children-resources (find-children-resources (document/id model) all-resources)
        operations (->> (or (domain/supported-operations model) [])
                        (map (fn [op]
                               (let [v (to-raml! op ctx)]
                                 [(keyword (domain/method op))
                                  (if (= v {})
                                    ;; we need this reader macro because of different behaviour
                                    ;; between the JS and JAVA YAML generators with nil nodes
                                    #?(:clj "" :cljs nil)
                                    v)])))
                        (into {}))]
    (-> {:displayName (document/name model)
         :is (common/find-traits model ctx :raml)
         :uriParameters (unparse-parameters (domain/parameters model) ctx)
         :description (document/description model)}
        (merge operations)
        (merge-children-resources children-resources ctx)
        (->> (common/with-amf-info model ctx "Resource"))
        (utils/clean-nils))))

(defn clean-default-object-body
  "Sometimes the type of the body is just a description, with this check we avoid generating a default body without associated properties"
  [response-body]
  (let [res (if (and (= "object" (:type response-body))
                     (nil? (:properties response-body)))
              (dissoc response-body :type)
              response-body)]
    (if (= {} res)
      ;; this is due to the particular behaviour of the clj-yaml and js-yaml
      #?(:clj "" :cljs nil)
      res)))

(defn project-bodies [bodies context]
  (if (= 1 (count bodies))
    ;; just one body, either it has a content type and become a  map or we plug it directly
    (let [body (first bodies)
          schema (to-raml! (domain/schema body) context)]
      (if-let [content-type (domain/media-type body)]
        (let [media-type (utils/safe-str content-type)
              schema (if (nil? schema)
                       ;; this might just be empty
                       {}
                       ;; it's a proper schema
                       schema)]
          ;; */* is the default media type generated automatialy when parsing OpenAPI documents
          ;; If it's the only one we find when generating RAML we can ignore it
          ;; and link the schema directly
          (if (not= media-type "*/*")
            {media-type (clean-default-object-body schema)}
            (clean-default-object-body schema)))
        (clean-default-object-body schema)))
    ;; If there are more than one, it must have a content-type
    (reduce (fn [acc body]
              (let [schema (to-raml! (domain/schema body) context)
                    schema (if (nil? schema)
                             ;; this might just be empty
                             {}
                             ;; it's a proper schema
                             schema)]
                (assoc acc
                       (-> body domain/media-type utils/safe-str)
                       (clean-default-object-body schema))))
            {}
            bodies)))

(defn group-responses [responses context]
  (->> responses
       (mapv (fn [response] [(document/name response) (to-raml! response context)]))
       (mapv (fn [[k v]] [k (if (= v {})
                             ;; we need this reader macro because of different behaviour
                             ;; between the JS and JAVA YAML generators with nil nodes
                             #?(:clj "" :cljs nil)
                             v)]))
       (into {})))

(defn unparse-query-parameters [request context]
  (if (nil? request) nil
      (if-let [parameters (domain/parameters request)]
        (unparse-parameters parameters context)
        nil)))

(defmethod to-raml domain/Operation [model context]
  (debug "Generating operation " (document/id model))
  (let [request (to-raml! (domain/request model) context)]
    (-> {:displayName (if (:is-trait context) nil (document/name model))
         :description (document/description model)
         :protocols (domain/scheme model)
         :responses (-> (domain/responses model)
                        (group-responses context))
         :is (common/find-traits model context :raml)}
        (merge request)
        (->> (common/with-amf-info model context "Operation"))
        utils/clean-nils)))

(defmethod to-raml domain/Response [model context]
  (debug "Generating response " (document/name model))
  (let [bodies (project-bodies (domain/payloads model) context)]
    (->> {:description (document/description model)
          :headers (unparse-parameters (domain/headers model) context)
          :body bodies}
         (common/with-amf-info model context "Response")
         (utils/clean-nils ))))


(defmethod to-raml :Request [model context]
  (debug "Generating request " (document/name model))
  (let [bodies (project-bodies (domain/payloads model) context)]
    (->> {:queryParameters (unparse-query-parameters model context)
          :body bodies
          :headers (unparse-parameters (domain/headers model) context)}
         (common/with-amf-info model context "RequestBody")
         utils/clean-nils)))

(defmethod to-raml domain/Type [model {:keys [generate-amf-info] :as context}]
  (debug "Generating type")
  (cond
    (and (not (:resolve-types context))
         (common/type-reference? model)) (common/type-reference-name model)
    :else                                (let [out-type (keywordize-keys
                                                         (shapes-parser/parse-shape
                                                          (domain/shape model) (assoc context :to-raml to-raml)))]
                                           (cond
                                             generate-amf-info (let [out-type (if (map? out-type)
                                                                                out-type
                                                                                {:type out-type})]
                                                                 (common/with-amf-info model context "TypeDeclaration" out-type))
                                             (map? out-type)  (if (= [:type] (keys out-type)) ;; If only type, we don't need the extra inheritance
                                                                (:type out-type)
                                                                out-type)
                                             :else out-type))))


(defmethod to-raml document/Includes [model {:keys [fragments expanded-fragments references document-generator type-hint]
                                             :as context
                                             :or {expanded-fragments (atom {})}}]
  (debug "Generating Includes")
  (let [target (document/target model)
        fragment (get fragments target)
        reference (->> references (filter (fn [ref] (= (document/id ref) target))) first)]
    (cond
      ;; usual path for RAML !includes and OpenAPI 'external' references
      (some? fragment)                         (if-let [expanded-fragment (get expanded-fragments target)]
                                                 expanded-fragment
                                                 (let [expanded-fragment (document-generator fragment context)]
                                                   (swap! expanded-fragments (fn [acc] (assoc acc target expanded-fragment)))
                                                   expanded-fragment))

      ;; OpenAPI internal references pointing to types
      (and (some? reference)
           (satisfies? domain/Type reference)) {:type (common/type-reference-name reference)}

      ;; OpenAPI internal references pointing to something we don't expose in RAML
      ;; we need a new way of poinitnt at this, we cannot use !include
      ;; we will use a new element and the (reference) annotation
      (some? reference)                        {(keyword "(reference)") target}

      ;; Reference to something we don't know about
      :else                                    (throw (new #?(:clj Exception :cljs js/Error)
                                                           (str "Cannot find fragment " target " for generation"))))))

(defmethod to-raml domain/DomainPropertySchema [model ctx]
  (debug "Generating DomainPropertySchema")
  (let [range (to-raml! (domain/range model) ctx)
        range (if (string? range) {:type range} range)
        name  (document/name model)
        domain (->> model domain/domain (map utils/domain-uri->node-name))
        description (document/description model)]
    (->> (merge range
                {:displayName name
                 :description description
                 :allowedTargets domain})
         (common/with-amf-info model ctx "AnnotationType")
         (utils/clean-nils))))

(defmethod to-raml nil [_ _]
  (debug "Generating nil")
  nil)
