(ns api-modeling-framework.generators.domain.openapi
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.generators.domain.shapes-json-schema :as shapes-parser]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.generators.domain.common :as common]
            [api-modeling-framework.generators.domain.utils :refer [send <-domain]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model)                                 model

    (satisfies? domain/DomainPropertySchema model)  domain/DomainPropertySchema

    (and (satisfies? document/Includes model)
         (satisfies? document/Node model))          document/Includes

    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       domain/APIDocumentation

    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))       domain/EndPoint

    (and (satisfies? domain/Operation model)
         (satisfies? document/Node model))       domain/Operation

    (and (satisfies? domain/Response model)
         (satisfies? document/Node model))       domain/Response

    (and (satisfies? domain/Parameter model)
         (satisfies? document/Node model))       domain/Parameter

    (and (satisfies? domain/Type model)
         (satisfies? document/Node model))       domain/Type

    :else                                        (type model)))

(defmulti to-openapi (fn [model ctx] (to-openapi-dispatch-fn model ctx)))

(defn clean-auto-generated [node]
  node
  (if (some? (:x-generated node))
    nil
    (cond
      (map? node) (let [cleaned (->> node
                                     (map (fn [[k v]] [k (clean-auto-generated v)]))
                                     (into {})
                                     utils/clean-nils)]
                    (if (= cleaned {})
                      nil
                      cleaned))
      (coll? node) (let [cleaned (->> node
                                      (map clean-auto-generated)
                                      (filter some?))]
                     (if (empty? cleaned) nil cleaned))
      :else (utils/ensure-not-blank node))))

(defn check-abstract [fragment model]
  (if (and (some? model)
           (satisfies? domain/DomainElement model)
           (domain/abstract model))
    (clean-auto-generated (assoc fragment :x-abstract-node true))
    fragment))

(defn with-annotations [model ctx generated]
  (if (map? generated)
    (let [annotations (document/additional-properties model)
          annotations-map (->> annotations
                               (filter (fn [annotation]
                                         (let [annotation-name (document/name annotation)]
                                           (and (not= annotation-name "traits")
                                                (not= annotation-name "is")
                                                (not= annotation-name "media-type")
                                                (not= annotation-name "requests")
                                                (not= annotation-name "abstract-node")
                                                (not= annotation-name "responses")))))
                               (map (fn [annotation]
                                      [(keyword (str "x-" (document/name annotation)))
                                       (->  annotation domain/object utils/jsonld->annotation)]))
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

;; Safe version of to-openapi that checks for includes
(defn to-openapi! [base-element {:keys [fragments expanded-fragments document-generator] :as ctx}]
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
      (to-openapi base-element ctx))))

(defn unparse-params [request ctx]
  (if (nil? request) []
      (let [params (or (domain/parameters request) [])]
        (map #(to-openapi! % ctx) params))))

(defn make-operation-abstract [trait-fragment]
  (-> trait-fragment
      clean-auto-generated
      (assoc :x-abstract-node true)
      (dissoc :operationId)))

(defmethod to-openapi domain/APIDocumentation [model ctx]
  (debug "Generating Swagger")
  (debug "Generating Info")
  (let [info (-> {:title (document/name model)
                  :description (document/description model)
                  :version (domain/version model)
                  :termsOfService (domain/terms-of-service model)}
                 utils/clean-nils
                 (utils/ensure :version ""))
        paths (->> (domain/endpoints model)
                   (map (fn [endpoint]
                          [(keyword (domain/path endpoint))
                           (to-openapi! endpoint ctx)]))
                   (into {}))
        traits (->> (common/model->traits (assoc ctx :abstract true) to-openapi!)
                    (map (fn [[trait-name trait-fragment]]
                           [trait-name (make-operation-abstract trait-fragment)]))
                    (into {}))
        types (common/model->types (assoc ctx :resolve-types true) to-openapi!)]
    (-> {:swagger "2.0"
         :host (domain/host model)
         :schemes (domain/scheme model)
         :basePath (domain/base-path model)
         :produces (if (= 1 (count (domain/content-type model)))
                     (first (domain/content-type model))
                     (domain/content-type model))
         :info info
         :consumes (if (= 1 (count (domain/accepts model)))
                     (first (domain/accepts model))
                     (domain/accepts model))
         :definitions types
         :x-baseUriParameters (unparse-params model ctx)
         :x-traits traits
         :x-annotationTypes (:annotations ctx)
         :paths paths}
        (->> (with-annotations model ctx)
             (common/with-amf-info model ctx "API"))
        utils/clean-nils
        (utils/ensure :paths {}))))

(defn method-name
  "Apparently, according to Methods/ meth03.raml example in the TCK, 'set' method must be supported. Let's add a mechanism to support custom method names"
  [x]
  (if (some? (get #{:get :post :patch :put :delete :head :options} (keyword x)))
    (keyword x)
    (keyword (str "x-method-" (utils/safe-str x)))))

(defmethod to-openapi domain/EndPoint [model ctx]
  (debug "Generating resource " (document/id model))
  (let [operations (domain/supported-operations model)
        parameters (unparse-params model ctx)
        end-point (->> operations
                       (map (fn [op] [(method-name (domain/method op)) (to-openapi! op ctx)]))
                       (into {}))
        traits (common/find-traits model ctx :openapi)
        end-point (-> end-point
                      (assoc :x-is traits)
                      (assoc :parameters parameters)
                      (->> (common/with-amf-info model ctx "PathItem"))
                      (utils/clean-nils))]
    end-point))

(defn unparse-bodies
  "Payloads == Bodies of the Request"
  [request ctx]
  (if (or (nil? request)
          (nil? (domain/payloads request))
          (empty? (domain/payloads request)))
    []
    (let [payloads (domain/payloads request)]
      (->> payloads
           (mapv (fn [payload]
                   (let [body (<-domain (domain/schema payload) ctx)
                         schema (to-openapi! body ctx)
                         parsed-body (->> {:name (if (and  (some? body)
                                                           (some? (document/name payload)))
                                                   (document/name payload)
                                                   "")
                                           :x-media-type (if (not= "*/*" (domain/media-type payload))
                                                           (domain/media-type payload)
                                                           nil)
                                           :schema schema}
                                          (common/with-amf-info payload ctx "ResponseBody")
                                          utils/clean-nils)]
                     (if (or (= {} parsed-body)
                             (= {:name ""} parsed-body))
                       nil
                       (assoc parsed-body :in "body")))))
           (filter some?)))))

(defmethod to-openapi domain/Operation [model ctx]
  (debug "Generating operation " (document/id model))
  (let [tags (->> (document/find-tag model document/api-tag-tag)
                  (map #(document/value %)))
        produces (domain/content-type model)
        traits  (common/find-traits model ctx :openapi)

        ;;;;;;;;;;;;;;;;
        ;; request
        request (domain/request model)
        headers (if (some? request)
                  (map #(to-openapi! % ctx) (domain/headers request))
                  [])
        parameters (if (some? request)
                     (unparse-params request ctx)
                     [])
        ;; we split the main request from the extra requests
        bodies (if (some? request)
                 (unparse-bodies request ctx)
                 [])
        [selected-main-body main-body] (->> [(->> bodies (filter #(= "*/*" (get % :x-media-type))) first)
                                             (->> bodies (filter #(= "application/json" (get % :x-media-type))) first)
                                             (first bodies)]
                                            (filter some?)
                                            (map (fn [body] [body (if (nil? (:schema body))
                                                                   (assoc body :schema {:type "object" :x-generated true})
                                                                   body)]))
                                            first)
        x-payloads (->> bodies
                        (filter (fn [body] (not= body selected-main-body)))
                        (mapv (fn [body]
                                (utils/clean-nils {:x-media-type (if (not= "*/*") (:x-media-type body) nil)
                                                   :schema (:schema body)}))))
        ;;;;;;;;;;;;;;;;

        ;; we process the responses
        responses (->> (domain/responses model)
                       (mapv (fn [response] [(document/name response) (to-openapi! response ctx)]))
                       (into {}))
        responses (if (and (or (nil? responses) (= {} responses))
                           (not (:abstract ctx)))
                    {:default {:x-generated true :description ""}}
                    responses)]
    (-> {:operationId (document/name model)
         :description (document/description model)
         :tags tags
         :x-is traits
         :schemes (domain/scheme model)
         :parameters (filter some? (concat headers parameters [main-body]))
         :x-request-payloads x-payloads
         :consumes (domain/accepts model)
         :produces produces
         :responses responses}
        (->> (common/with-amf-info model ctx "Operation"))
        utils/clean-nils)))

(defmethod to-openapi domain/Response [model ctx]
  (debug "Generating response " (document/name model))
  (let [;; unparse-bodies generates body params, we need to adapt the result
        ;; picking the components we need for the main payload in the response
        ;; and the different x-response-payloads
        bodies (unparse-bodies model ctx)

        main-body (->> [(->> bodies (filter #(= "*/*" (get % :x-media-type))) first)
                        (->> bodies (filter #(= "application/json" (get % :x-media-type))) first)
                        (first bodies)]
                       (filter some?)
                      first)
        main-payload (:schema main-body)
        x-payloads (->> bodies
                        (filter (fn [body] (not= body main-body)))
                        (map (fn [{:keys [x-media-type schema]}]
                               (utils/clean-nils {:x-media-type x-media-type
                                                  :schema schema}))))]

    (-> {;; description for responses is mandatory in openapi
         :description (if (nil? (document/description model))
                        ""
                        (document/description model))
         :schema main-payload
         :x-media-type (if (not= "*/*" (:x-media-type main-body)) (:x-media-type main-body) nil)
         :x-response-payloads x-payloads}
        (->> (common/with-amf-info model ctx "Response"))
        utils/clean-nils)))

(defmethod to-openapi domain/Parameter [model ctx]
  (debug "Generating parameter " (document/name model))
  (let [base {:description (document/description model)
              :name (or (document/name model) "")
              :required (domain/required model)
              :in (domain/parameter-kind model)}
        type-info (merge (keywordize-keys (shapes-parser/parse-shape (domain/shape model) ctx)))]
    (-> (merge base type-info)
        utils/clean-nils)))

(defmethod to-openapi domain/Type [model context]
  (debug "Generating type")
  (common/with-amf-info
    model
    context "TypeDeclaration"
    (keywordize-keys (shapes-parser/parse-shape (domain/shape model) context))))

(defmethod to-openapi document/Includes [model {:keys [fragments expanded-fragments document-generator references]
                                                :as context
                                                :or {expanded-fragments (atom {})}}]
  (let [fragment-target (document/target model)
        fragment (get fragments fragment-target)
        reference (->> references
                       (filter #(= (document/id %) fragment-target))
                       first)]
    (cond
      ;; if it is a fragment, is a link to an external node, I need to generate the document
      (some? fragment) (if-let [expanded-fragment (get expanded-fragments fragment-target)]
                         expanded-fragment
                         (let [expanded-fragment (document-generator fragment context)]
                           (swap! expanded-fragments (fn [acc] (assoc acc fragment-target expanded-fragment)))
                           expanded-fragment))
      ;; If it is a reference, is a link to internally defined node, I just need the reference
      (some? reference) {:$ref fragment-target}
      ;; Unknown reference @todo Should I throw an exception in this case?
      :else             {:$ref fragment-target})))

(defmethod to-openapi domain/DomainPropertySchema [model ctx]
  (debug "Generating DomainPropertySchema")
  (let [range (to-openapi! (domain/range model) ctx)
        name  (document/name model)
        domain (->> model domain/domain (map utils/domain-uri->openapi-node-name))
        description (document/description model)]
    (->> (merge range
                {:displayName name
                 :description description
                 :allowedTargets domain})
         (common/with-amf-info model ctx "DomainPropertySchema")
         (utils/clean-nils))))

(defmethod to-openapi nil [_ _]
  (debug "Generating nil")
  nil)
