(ns api-modelling-framework.parser.domain.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.common :as common]
            [api-modelling-framework.parser.domain.json-schema-shapes :as shapes]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.model.vocabulary :as v]
            [clojure.set :as set]
            [cemerick.url :as url]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))


(def properties-map
  {
   :basePath #{:swagger}
   :consumes #{:swagger :operation}
   :contact #{:info}
   :default #{:type}
   :definitions #{:swagger}
   :delete #{:path-item}
   :deprecated #{:operation}
   :description #{:info :operation :response :type}
   :enum #{:type}
   :examples #{:response}
   :exclusiveMaximum #{:type}
   :exclusiveMinimum #{:type}
   :externalDocs #{:swagger :operation}
   :format #{:type}
   :get #{:path-item}
   :head #{:path-item}
   :headers #{:response}
   :host #{:swagger}
   :info #{:swagger}
   :license #{:info}
   :maxItems #{:type}
   :maxLength #{:type}
   :maxProperties #{:type}
   :maximum #{:type}
   :minItems #{:type}
   :minLength #{:type}
   :minProperties #{:type}
   :minimum #{:type}
   :multipleOf #{:type}
   :operationId #{:operation}
   :options #{:path-item}
   :parameters #{:swagger :path-item :operation}
   :patch #{:path-item}
   :paths #{:swagger}
   :pattern #{:type}
   :post #{:path-item}
   :produces #{:swagger :operation}
   :put #{:path-item}
   :required #{:type}
   :responses #{:swagger :operation}
   :schema #{:response}
   :schemes #{:swagger :operation}
   :security #{:swagger :operation}
   :securityDefinitions #{:swagger}
   :summary #{:operation}
   :swagger #{:swagger}
   :tags #{:swagger :operation}
   :termsOfService #{:info}
   :title #{:info :type}
   :type #{:type}
   :uniqueItems #{:type}
   :version #{:info}
   })

(defn guess-type-from-predicates [x]
  (->> [(fn [x] (when (string/starts-with? (utils/safe-str x) "/") #{:paths}))
        (fn [x] (when (some? (re-matches #"^\d+$" (utils/safe-str x))) #{:responses}))]
       (mapv (fn [p] (p x)))
       (filterv some?)
       first))

(defn guess-type [node]
  (let [node-types (->> node
                        (mapv (fn [[k _]] (get properties-map k (guess-type-from-predicates k))))
                        (filterv some?))
        node-type (first (if (empty? node-types) [] (apply set/intersection node-types)))]
    (or node-type :undefined)))

(defn parse-ast-dispatch-function [node context]
  (cond
    (some? (syntax/<-location node))         :fragment
    (some? (:type-hint context))             (:type-hint context)
    (some? (get node :$ref))                 :local-ref
    (nil? node)                              :undefined
    :else                                    (guess-type node)))

(defmulti parse-ast (fn [node context] (parse-ast-dispatch-function node context)))

(defn generate-is-annotation-sources [annotation-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/is-annotation")
        is-trait-tag (document/->IsAnnotationTag source-map-id annotation-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [is-trait-tag] [])]))

(defn parse-annotation-ast [p model {:keys [base-uri annotations parsed-location]}]
  (let [annotation-name (-> p
                            utils/safe-str
                            (string/replace #"x-" ""))
        annotation (get @annotations annotation-name)
        schema-id (str base-uri "#" (url/url-encode annotation-name))]

    (when (and (nil? annotation)
               (not= annotation-name "traits")
               (not= annotation-name "is")
               (not= annotation-name "media-type")
               (not= annotation-name "requests")
               (not= annotation-name "responses"))
      (swap! annotations (fn [acc]
                           (assoc acc annotation-name (domain/map->ParsedDomainPropertySchema {:id schema-id
                                                                                               :name annotation-name
                                                                                               :sources (generate-is-annotation-sources annotation-name schema-id parsed-location)
                                                                                               :range (shapes/parse-scalar
                                                                                                       (str base-uri "#" annotation-name "/range")
                                                                                                       (v/xsd-ns "string"))})))))
    (domain/map->ParsedDomainProperty {:id (document/id annotation)
                                       :name annotation-name
                                       :object (utils/annotation->jsonld (document/id annotation) model)})))

(defn annotation? [x] (string/starts-with? (utils/safe-str x) "x-"))

(defn with-annotations [node ctx model]
  (let [parsed-annotations (->> node
                                (filter (fn [[k v]] (annotation? k)))
                                (map (fn [[k v]] (parse-annotation-ast k v ctx))))]
    (if (> (count parsed-annotations) 0)
      (if (some? (:properties model))
        (assoc-in model [:properties :additional-properties] parsed-annotations)
        (assoc model :additional-properties parsed-annotations))
      model)))

(defn generate-inline-fragment-parsed-sources [parsed-location fragment-name fragment-location]
  (let [source-map-id (str parsed-location "/source-map/inline-fragment/" fragment-name)
        inline-fragment-parsed-tag (document/->InlineFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map")
                                   fragment-location
                                   [inline-fragment-parsed-tag]
                                   [])]))

(defn generate-extend-include-fragment-sources [parsed-location fragment-location]
  (let [source-map-id (str parsed-location "/source-map/inline-fragment")
        parsed-tag (document/->ExtendIncludeFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map")
                                   fragment-location
                                   [parsed-tag]
                                   [])]))

(defn generate-is-trait-sources [trait-name location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/is-trait")
        is-trait-tag (document/->IsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [is-trait-tag] [])]))

(defn process-traits [node {:keys [location parsed-location] :as context}]
  (debug "Processing " (count (:x-traits node [])) "traits")
  (let [location (str location "/traits")
        parsed-location (str parsed-location "/traits")
        nested-context (-> context (assoc :location location) (assoc :parsed-location parsed-location))]
    (->> (:x-traits node {})
         (reduce (fn [acc [trait-name trait-node]]
                   (debug (str "Processing trait " trait-name))
                   (let [fragment-name (url/url-encode (utils/safe-str trait-name))
                         references (get nested-context :references {})
                         trait-fragment (parse-ast trait-node (-> nested-context
                                                                  (assoc :method fragment-name)
                                                                  (assoc :references (merge references acc))
                                                                  (assoc :location location)
                                                                  (assoc :parsed-location parsed-location)
                                                                  (assoc :type-hint :operation)))
                         trait-fragment (assoc trait-fragment :id (str parsed-location "/" fragment-name))
                         sources (or (-> trait-fragment :sources) [])
                         sources (concat sources (generate-is-trait-sources fragment-name
                                                                            (str location "/" fragment-name)
                                                                            (str parsed-location "/" fragment-name)))
                         parsed-trait (assoc trait-fragment :sources sources)]
                     (assoc acc (keyword trait-name) parsed-trait)))
                 {}))))

(defn process-types [node {:keys [location alias-chain] :as context}]
  (debug "Processing " (count (:definitions node [])) " types")
  (->> (:definitions node {})
       (reduce (fn [acc [type-name type-node]]
                 (debug (str "Processing type " type-name))
                 (let [type-id (common/type-reference location (url/url-encode (utils/safe-str type-name)))
                       type-alias (keyword (str "#" (last (string/split type-id #"#"))))
                       type-fragment (parse-ast type-node (-> context
                                                              ;; the physical location matches the structure of the OpennAPI document
                                                              (assoc :location (utils/path-join location "/definitions/" type-name))
                                                              ;; the logical location is the actual URI of the type, we use common notation
                                                              ;; encapsulated in the common/type-reference function
                                                              (assoc :parsed-location type-id)
                                                              (assoc :is-fragment false)
                                                              (assoc :type-hint :type)))
                       sources (or (-> type-fragment :sources) [])
                       sources (concat sources (common/generate-is-type-sources type-name type-id type-id))
                       parsed-type (-> type-fragment
                                       (assoc :name (utils/safe-str type-name))
                                       (assoc :sources sources)
                                       (assoc :id type-id))]
                   (assoc acc type-id parsed-type)))
               {})))

(defn generate-parsed-node-sources [node-name location parsed-location]
  (let [source-map-parsed-location (str parsed-location "/source-map/" node-name)
        node-parsed-tag (document/->NodeParsedTag source-map-parsed-location location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag] [])]))

(defn generate-open-api-tags-sources [tags location parsed-location]
  (let [tags (or tags [])]
    (->> tags
         (mapv (fn [i tag]
                 (let [parsed-location (str parsed-location "/source-map/api-tags-" i "/tag")]
                   (document/->DocumentSourceMap
                    (str parsed-location "/source-map/api-tags-" i)
                    location
                    [(document/->APITagTag parsed-location tag)]
                    [])))
               (range 0 (count tags))))))

(defn find-extend-tags [{:keys [location parsed-location references] :as context}]
  (->> references
       (mapv (fn [[extend-name parsed-domain-element]]
               (condp = (:fragment-node parsed-domain-element)
                 :trait (generate-inline-fragment-parsed-sources (str parsed-location "/traits")
                                                                 (name extend-name)
                                                                 (:id parsed-domain-element))
                 (generate-inline-fragment-parsed-sources (str parsed-location "/declares")
                                                          (name extend-name)
                                                          (:id parsed-domain-element)))))
       flatten
       (filter some?)))

(defn parse-params [parameters {:keys [location parsed-location is-fragment] :as context}]
  (->> (or parameters [])
       (filterv (fn [parameter] (not= "body" (:in parameter))))
       (mapv (fn [i parameter]
               (let [name (:name parameter)
                     location (str location "[" i "]")
                     parsed-location (str parsed-location "/" (url/url-encode name))
                     node-sources (generate-parsed-node-sources "parameter" location parsed-location)]
                 {:id parsed-location
                  :name (if (= name "") nil name)
                  :description (:description parameter)
                  :sources node-sources
                  :parameter-kind (:in parameter)
                  :required (:required parameter)
                  :shape (shapes/parse-type (-> parameter
                                                (dissoc :name)
                                                (dissoc :description)
                                                (dissoc :in))
                                            (-> context
                                                (assoc :parse-ast parse-ast)
                                                (assoc :location location)
                                                (assoc :parsed-location parsed-location)))}))
             (range 0 (count parameters)))
       (mapv (fn [properties] (domain/map->ParsedParameter properties)))))

(defmethod parse-ast :swagger [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing swagger")
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        trait-tags (find-extend-tags (-> context
                                         (assoc :location location)
                                         (assoc :parsed-location parsed-location)))
        sources (concat (generate-parsed-node-sources "root" location parsed-location)
                        trait-tags)
        endpoints (parse-ast (:paths node) (-> context
                                               (assoc :type-hint :paths)
                                               (assoc :parsed-location parsed-location)
                                               (assoc :location location)))
        parameters (parse-params (:x-baseUriParameters node) (-> context
                                                                 (assoc :is-fragment false)
                                                                 (assoc :location (str location "/parameters"))
                                                                 (assoc :parsed-location (str parsed-location "/parameters"))))
        properties {:id parsed-location
                    :host (:host node)
                    :scheme (filter some? (flatten [(:schemes node)]))
                    :base-path (:basePath node)
                    :accepts (filter some? (flatten [(:consumes node)]))
                    :parameters parameters
                    :content-type (filter some? (flatten [(:produces node)]))
                    :provider nil
                    :license nil
                    :endpoints endpoints}
        node-info-properties (if (some? (:info node))
                               (parse-ast (:info node) (-> context
                                                           (assoc :type-hint :info)
                                                           (assoc :is-fragment true)
                                                           (assoc :parsed-location parsed-location)
                                                           (assoc :location location)))
                               {})
        properties (merge (utils/clean-nils properties) (utils/clean-nils node-info-properties))
        properties (assoc properties :sources sources)]
    (domain/map->ParsedAPIDocumentation (utils/clean-nils properties))))

(defmethod parse-ast :info [node {:keys [location parsed-location is-fragment]}]
  (debug "Parsing info")
  (let [location (str location "info")]
    (domain/map->ParsedAPIDocumentation {:name (:title node)
                                         :fragment-node :info
                                         :description (:description node)
                                         :version (if (not= "" (:version node)) (:version node) nil)
                                         :sources (generate-parsed-node-sources "info" location parsed-location)
                                         :terms-of-service (:termsOfService node)})))

(defmethod parse-ast :paths [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing paths")
  (let [location (str location "paths")
        paths-sources (generate-parsed-node-sources "paths" location parsed-location)
        nested-resources (utils/extract-nested-resources node)]
    (mapv (fn [i {:keys [path resource]}]
            (let [context (-> context
                              (assoc :type-hint :path-item)
                              (assoc :location (str location "/" (url/url-encode path)))
                              (assoc :parsed-location (str parsed-location "/end-points/" i))
                              (assoc :path path)
                              (assoc :paths-sources paths-sources))]
              (parse-ast resource context)))
          (range 0 (count nested-resources))
          nested-resources)))

(defn generate-extends-trait-sources [trait-name location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/extend-trait")
        extends-trait-tag (document/->ExtendsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [extends-trait-tag] [])]))

(defn parse-traits [resource-id node references {:keys [location parsed-location]}]
  (let [traits (filter some? (flatten [(:x-is node [])]))]
    (->> traits
         (mapv (fn [trait-name]
                 [trait-name (-> references
                                 (get (keyword trait-name)))]))
         (mapv (fn [i [trait-name trait]]
                 (if (some? trait)
                   (let
                       [extend-id (str parsed-location "/extends/" (url/url-encode trait-name))
                        extend-location (str location "/x-is/" i)
                        node-parsed-source-map (generate-parsed-node-sources "x-is" location extend-id)
                        extends-trait-source-map (generate-extends-trait-sources trait-name extend-location extend-id)]
                     (document/map->ParsedExtends {:id extend-id
                                                   :sources (concat node-parsed-source-map
                                                                    extends-trait-source-map)
                                                   :target (document/id trait)
                                                   :label "trait"
                                                   :arguments []}))
                   (throw (new #?(:clj Exception :cljs js/Error)
                               (str "Cannot find trait '" trait-name "' to extend in node '" resource-id "'")))))
               (range 0 (count traits))))))

(defmethod parse-ast :path-item [node {:keys [location parsed-location is-fragment path paths-sources references] :as context}]
  (debug "Parsing path-item")
  (when (nil? path)
    (throw (new #?(:clj Exception :cljs js/Error) "Cannot parse path-item object without contextual path information")))
  (let [location (str location "/" (url/url-encode path))
        parsed-location (str parsed-location "/" (url/url-encode path))
        traits (parse-traits parsed-location node references (assoc context :parsed-location parsed-location))
        extended-operations (->> node keys (filter (fn [x] (string/starts-with? (utils/safe-str x) "x-method-"))))
        operations (->> [:get :put :post :delete :options :head :patch]
                        (concat extended-operations)
                        (mapv (fn [op] (if-let [method-node (get node op)]
                                        (parse-ast method-node (-> context
                                                                   (assoc :type-hint :operation)
                                                                   (assoc :method (name op))
                                                                   (assoc :location location)
                                                                   (assoc :parsed-location parsed-location)))
                                        nil)))
                        (filterv some?))
        parameters (parse-params (:parameters node) (-> context
                                                        (assoc :is-fragment false)
                                                        (assoc :location (str location "/parameters"))
                                                        (assoc :parsed-location (str parsed-location "/parameters"))))
        properties {:path path
                    :sources (concat (generate-parsed-node-sources "path-item" location parsed-location) (or paths-sources []))
                    :id parsed-location
                    :parameters parameters
                    :supported-operations operations
                    :extends traits}]
    (domain/map->ParsedEndPoint properties)))

(defn parse-body [parameters {:keys [location parsed-location is-fragment] :as context}]
  (->> (or parameters [])
       (filterv (fn [parameter] (= "body" (:in parameter))))
       (mapv (fn [i parameter]
               (let [x-media-type (:x-media-type parameter "*/*")
                     name (:name parameter)
                     location (str location "/parameters[" i "]")
                     parsed-location (str parsed-location "/body")
                     node-sources (generate-parsed-node-sources "body" location parsed-location)]
                 [x-media-type
                  {:id parsed-location
                   :name (if (= name "") nil name)
                   :description (:description parameter)
                   :sources node-sources
                   :shape (shapes/parse-type (:schema parameter)
                                             (-> context
                                                 (assoc :is-fragment false)
                                                 (assoc :type-hint :type)
                                                 (assoc :location location)
                                                 (assoc :parse-ast parse-ast)
                                                 (assoc :parsed-location parsed-location)))}]))
             (range 0 (count parameters)))
       (mapv (fn [[media-type properties]]
               {:media-type media-type
                :body (domain/map->ParsedType properties)}))
       first))

(defn filter-empty-payloads [payloads]
  (->> payloads
       (filter (fn [payload]
                 (or (and (some? (domain/schema payload))
                          (some? (-> payload domain/schema domain/shape)))
                     (and (not= "*/*" (domain/media-type payload))
                          (some? (domain/media-type payload))))))))

(defn parse-request [node {:keys [location parsed-location] :as context}]
  (let [parameters (parse-params (:parameters node) (-> context
                                                        (assoc :is-fragment false)
                                                        (assoc :location (str location "/parameters"))
                                                        (assoc :parsed-location (str parsed-location "/parameters"))))
        x-request-description (if (not= "" (:x-request-description node)) (:x-request-description node) nil)
        headers (->> parameters (filterv #(= "header" (:parameter-kind %))))
        parameters (->> parameters (filterv #(not= "header" (:parameter-kind %))))
        body (parse-body (:parameters node) (-> context
                                                (assoc :is-fragment false)
                                                (assoc :location (str location "/parameters"))
                                                (assoc :parsed-location (str parsed-location "/body"))))

        payload (if (some? body)
                  (domain/map->ParsedPayload {:id (utils/path-join parsed-location "/main-payload")
                                              :media-type (:media-type body)
                                              :name (-> body :body :name)
                                              :description (-> body :body :description)
                                              :schema (-> (:body body)
                                                          (assoc :name nil)
                                                          (assoc :description nil)
                                                          (assoc :media-type nil))})
                  nil)
        ;; we support multiple request per operation, OpenAPI only supports 1 we need the additional x-requests
        x-payloads (->> (get node :x-request-payloads [])
                        (mapv (fn [i {:keys [schema x-media-type]}]
                                (let [x-payload-id (utils/path-join parsed-location (str "x-request-payload" i))
                                      location (utils/path-join location (str "x-request-payloads[" i "]"))
                                      properties {:id x-payload-id
                                                  :media-type x-media-type
                                                  :schema (parse-ast schema (-> context
                                                                                (assoc :location location)
                                                                                (assoc :parsed-location parsed-location)
                                                                                (assoc :type-hint :type)))}]
                                  (domain/map->ParsedPayload (utils/clean-nils properties))))
                              (range 0 (count (get node :x-request-payloads []))))
                        (filter some?))
        payloads (->> (concat [payload] x-payloads)
                      (filter some?)
                      (filter-empty-payloads))
        request-id (str parsed-location "/request")]
    (if (empty? (concat headers parameters payload))
      nil
      (domain/map->ParsedRequest {:id request-id
                                  :description x-request-description
                                  :sources (generate-parsed-node-sources "request" location request-id)
                                  :headers headers
                                  :parameters parameters
                                  :payloads payloads}))))

(defn method-name
  "Apparently, according to Methods/ meth03.raml example in the TCK, 'set' method must be supported. Let's add a mechanism to support custom method names"
  [x]
  (let [x (utils/safe-str x)]
    (if (string/starts-with? x "x-method-")
      (last (string/split x #"x-method-"))
      x)))


(defmethod parse-ast :operation [node {:keys [location parsed-location is-fragment method references] :as context}]
  (debug "Parsing method " method)
  (let [method (method-name method)
        location (str location "/" method)
        parsed-location (str location "/" method)
        next-context (-> context
                         (assoc :is-fragment false)
                         (assoc :parsed-location parsed-location)
                         (assoc :location location))
        traits (parse-traits parsed-location node references next-context)
        node-parsed-source-map (generate-parsed-node-sources method location parsed-location)
        api-tags (generate-open-api-tags-sources (:tags node) location parsed-location)
        request (parse-request node context)
        properties {:id parsed-location
                    :method method
                    :sources (concat node-parsed-source-map api-tags)
                    :name (:operationId node)
                    :description (:description node)
                    :scheme (:schemes node)
                    :accepts (filter some? (flatten [(:consumes node)]))
                    :content-type (:produces node)
                    :request request
                    :responses (parse-ast (:responses node) (-> context
                                                                (assoc :type-hint :responses)
                                                                (assoc :location location)
                                                                (assoc :parsed-location parsed-location)
                                                                (assoc :is-fragment false)))}]
    (domain/map->ParsedOperation properties)))

(defmethod parse-ast :responses [node {:keys [location parsed-location is-fragment x-response-bodies-with-media-types] :as context}]
  (debug "Parsing responses")
  (->> node
       (mapv (fn [[key response]]
               (parse-ast response (-> context
                                       (assoc :type-hint :response)
                                       (assoc :response-key key)
                                       (assoc :is-fragment false)
                                       (assoc :location (str location "/responses"))
                                       (assoc :parsed-location (str parsed-location "/responses"))))))
       (filter some?)))

(defmethod parse-ast :response [node {:keys [location parsed-location is-fragment response-key produces] :as context}]
  (debug "Parsing response " response-key)
  (if (node :x-generated)
    ;; generated response, don't process
    nil
    ;; user-generated content, process
    (let [response-key (name response-key)
          response-id (str parsed-location "/" response-key)
          location (str location "/" response-key)
          is-status (some? (re-find #"^\d+$" (name response-key)))
          node-parsed-source-map (generate-parsed-node-sources (str "response-" response-key) response-id parsed-location)
          body (parse-ast (:schema node) (-> context
                                             (assoc :location location)
                                             (assoc :parsed-location response-id)
                                             (assoc :type-hint :type)))
          x-media-type (:x-media-type node)

          payload (domain/map->ParsedPayload {:id (utils/path-join parsed-location "main-payload")
                                              :media-type (if (some? x-media-type) x-media-type "*/*")
                                              :schema body})
          ;; we support multiple payloads per response, OpenAPI only supports 1, we need the additional x-response-payloads
          x-payloads (->> (get node :x-response-payloads [])
                          (mapv (fn [i {:keys [schema x-media-type]}]
                                  (let [x-payload-id (utils/path-join parsed-location (str "x-response-payload" i))
                                        location (utils/path-join location (str "x-response-payloads[" i "]"))
                                        properties {:id x-payload-id
                                                    :media-type x-media-type
                                                    :schema (parse-ast schema (-> context
                                                                                  (assoc :location location)
                                                                                  (assoc :parsed-location parsed-location)
                                                                                  (assoc :type-hint :type)))}]
                                    (domain/map->ParsedPayload (utils/clean-nils properties))))
                                (range 0 (count (get node :x-response-payloads []))))
                          (filter some?))

          payloads (filter-empty-payloads (concat [payload] x-payloads))
          properties {:id response-id
                      :description (utils/ensure-not-blank (:description node))
                      :sources node-parsed-source-map
                      :status-code (if is-status (name response-key) nil)
                      :name response-key
                      :payloads payloads}]
      (domain/map->ParsedResponse properties))))



(defmethod parse-ast :type [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing type")
  (let [shape (shapes/parse-type node (-> context
                                          (assoc :parsed-location parsed-location)
                                          (assoc :parse-ast parse-ast)))
        type-id (str (get shape "@id") "/wrapper")]
    ;; ParsedType nodes just wrap the JSON-LD description for the shape.
    ;; They should not generate stand-alone nodes in the JSON-LD domain model, the node IS the shape
    (domain/map->ParsedType {:id type-id
                             :shape shape})))

(defn parse-included-fragment
  "Generates a Includes element from an $ref occurrence in the AST. The $ref tag might point to a local or remote reference.
   If it points to the remote reference, the encoded element in the remote fragment and the remote fragment need to be passed as
   arguments, the function will update  the list of fragments with the parsed one and will generate the right include or an abstract
   element extending the reference depending on the type of included node.
   In the case of a local reference, no parsed fragment must be passed and only the right output node will be generated without
   updating the list of references.
   - See parse-ast :fragment and parse-ast :local-ref for more info"
  [fragment-location encoded-element parsed-fragment {:keys [location parsed-location is-fragment fragments type-hint document-parser]
                                                      :or {fragments (atom {})}}]
  (let [encoded-element-sources (-> encoded-element :sources)
        clean-encoded-element (condp = type-hint
                                ;; this information is sensitive to the context, can never be in the fragment
                                ;; but we will add it to the abstract element we will generate below
                                :operation (-> encoded-element
                                               (assoc :method nil)
                                               (assoc :sources nil))
                                :path-item (-> encoded-element
                                               (assoc :path nil)
                                               (assoc-in :sources nil))
                                encoded-element)
        parsed-location (str parsed-location "/includes")
        extends [(document/map->ParsedExtends {:id parsed-location
                                               :sources (generate-extend-include-fragment-sources parsed-location fragment-location)
                                               :target fragment-location
                                               :label "$ref"
                                               :arguments []})]]
    (when (some? parsed-fragment)
      (swap! fragments (fn [acc]
                         (if (some? (get acc fragment-location))
                           acc
                           (assoc acc fragment-location (assoc parsed-fragment :encodes clean-encoded-element))))))
    ;; this is the abstract element that will hold the reference to the extended element.
    ;; We include here the local information we have extracted from the extended element (source and path/method)
    (condp = type-hint
      :operation (domain/map->ParsedOperation {:id parsed-location
                                               :method (utils/safe-str (-> encoded-element :method))
                                               :sources encoded-element-sources
                                               :extends extends})
      :path-item (domain/map->ParsedEndPoint {:id parsed-location
                                              :path (-> encoded-element :path)
                                              :extends extends
                                              :sources encoded-element-sources})
      (let [properties {:id parsed-location
                        :label "$ref"
                        :target fragment-location}]
        (document/map->ParsedIncludes properties)))))

(defmethod parse-ast :fragment [node {:keys [document-parser] :as context}]
  (let [fragment-location (syntax/<-location node)
        parsed-fragment (document-parser node context)
        encoded-element (document/encodes parsed-fragment)]
    (parse-included-fragment fragment-location encoded-element parsed-fragment context)))

(defmethod parse-ast :local-ref [node {:keys [fragments references] :as context}]
  (let [fragment-location (get node :$ref)
        fragment (get references fragment-location)]
    (if (nil? fragment)
      (throw (new #?(:cljs js/Error :clj Exception) (str "Unknown $ref " (get node :$ref))))
      (parse-included-fragment fragment-location fragment nil context))))

(defmethod parse-ast :undefined [_ _]
  (debug "Parsing undefined")
  nil)
