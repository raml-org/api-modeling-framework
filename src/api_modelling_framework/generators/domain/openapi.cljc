(ns api-modelling-framework.generators.domain.openapi
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.generators.domain.shapes-json-schema :as shapes-parser]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.generators.domain.common :as common]
            [api-modelling-framework.generators.domain.utils :refer [send <-domain]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model)                                 model

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

(defn includes? [x]
  (if (and (some? x) (some? (document/extends x)) (= 1 (count (document/extends x))))
    (let [extended (document/extends x)
          included-tag (first (document/find-tag (first extended) document/extend-include-fragment-parsed-tag))]
      (some? included-tag))
    false))

;; Safe version of to-openapi that checks for includes
(defn to-openapi! [x {:keys [fragments expanded-fragments document-generator] :as ctx}]
  (if (includes? x) ;; is this node merging something?
    (let [fragment-target (document/target (first (document/extends x)))
          fragment (get fragments fragment-target)]
      (if (nil? fragment)
        (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " fragment-target " for generation")))
        ;; we first check in the expansion cache
        (if-let [expanded-fragment (get expanded-fragments fragment-target)]
          expanded-fragment
          ;; not in the cache we compute the value
          (let [encoded-fragment (document/encodes fragment)
                encoded-fragment-properties (:properties encoded-fragment)
                encoded-fragment-properties (reduce (fn [acc k]
                                                      (let [v (get x k)]
                                                        (if (nil? v) acc (assoc acc k v))))
                                                    encoded-fragment-properties
                                                    (keys x))
                encoded-fragment-properties (assoc encoded-fragment-properties :extends [])
                encoded-fragment (assoc encoded-fragment :properties encoded-fragment-properties)
                encoded-fragment (assoc encoded-fragment :extends [])
                fragment (assoc fragment :encodes encoded-fragment)
                expanded-fragment (document-generator fragment ctx)]
            ;; before returning the expanded fragment, we saved it in the cache
            (swap! expanded-fragments (fn [acc] (assoc acc fragment-target expanded-fragment)))
            expanded-fragment))))
    ;; Nothing to merge
    (to-openapi x ctx)))

(defmethod to-openapi domain/APIDocumentation [model ctx]
  (debug "Generating Swagger")
  (let [info (-> {:title (document/name model)
                  :description (document/description model)
                  :version (domain/version model)
                  :termsOfService (domain/terms-of-service model)}
                 utils/clean-nils)
        info (if (= {} info)
               nil
               (do (debug "Generating Info")
                   info))
        paths (->> (domain/endpoints model)
                   (map (fn [endpoint]
                          [(keyword (domain/path endpoint))
                           (to-openapi! endpoint ctx)]))
                   (into {}))]
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
         :definitions (common/model->types (assoc ctx :resolve-types true) to-openapi!)
         :x-traits (common/model->traits (assoc ctx :abstract true) to-openapi!)
         :paths paths}
        utils/clean-nils)))


(defmethod to-openapi domain/EndPoint [model ctx]
  (debug "Generating resource " (document/id model))
  (let [operations (domain/supported-operations model)
        end-point (->> operations
                       (map (fn [op] [(keyword (domain/method op)) (to-openapi! op ctx)]))
                       (into {}))]
    (-> end-point
        (assoc :x-is (common/find-traits model ctx))
        (utils/clean-nils))))

(defn unparse-bodies [request ctx]
  (if (or (nil? request)
          (nil? (domain/payloads request))
          (empty? (domain/payloads request)))
    nil
    (let [payloads (domain/payloads request)]
      (->> payloads
           (mapv (fn [payload]
                   (let [body (<-domain (domain/schema payload) ctx)
                         schema (to-openapi! body ctx)
                         parsed-body (-> {:name (if (and  (some? body)
                                                          (some? (document/name body)))
                                                  (document/name body)
                                                  "body")
                                          :description (document/description payload)
                                          :x-media-type (domain/media-type payload)
                                          :schema schema}
                                         utils/clean-nils)]
                     (if (or (= {} parsed-body)
                             (= {:name "body", :x-media-type "*/*"} parsed-body))
                       nil
                       (assoc parsed-body :in "body")))))))))

(defn unparse-params [request ctx]
  (if (nil? request) []
      (let [params (or (domain/parameters request) [])]
        (map #(to-openapi! % ctx) params))))

(defmethod to-openapi domain/Operation [model ctx]
  (debug "Generating operation " (document/id model))
  (let [tags (->> (document/find-tag model document/api-tag-tag)
                  (map #(document/value %)))
        produces (domain/content-type model)
        traits  (common/find-traits model ctx)
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
        main-body (-> [(first bodies)
                       (->> bodies (filter #(= "*/*" (get % :x-media-type))) first)
                       (->> bodies (filter #(= "application/json" (get % :x-media-type))) first)]
                      first)
        x-requests (->> bodies
                        (filter (fn [body] (not= body main-body)))
                        (mapv (fn [body]
                                {:x-media-type (:x-media-type body)
                                 :parameters [(dissoc body :x-media-type)]})))

        ;; we process the responses
        responses (->> (domain/responses model)
                       (mapv (fn [response] [(document/name response) (to-openapi! response ctx)]))
                       (into {}))
        responses (if (and (or (nil? responses) (= {} responses))
                           (not (:abstract ctx)))
                    {:default {:description ""}}
                    responses)]
    (-> {:operationId (document/name model)
         :description (document/description model)
         :tags tags
         :x-is traits
         :schemes (domain/scheme model)
         :parameters (filter some? (concat headers parameters [main-body]))
         :x-requests x-requests
         :consumes (domain/accepts model)
         :produces produces
         :responses responses}
        utils/clean-nils)))

(defmethod to-openapi domain/Response [model ctx]
  (debug "Generating response " (document/name model))
  (let [bodies (unparse-bodies model ctx)
        main-body (-> [(first bodies)
                       (->> bodies (filter #(= "*/*" (get % :x-media-type))) first)
                       (->> bodies (filter #(= "application/json" (get % :x-media-type))) first)]
                      first)
        x-responses (->> bodies
                         (filter (fn [body] (not= body main-body)))
                         (mapv (fn [body]
                                 {:x-media-type (:x-media-type body)
                                  :schema (:schema body)})))]

    (-> {:description (or (document/description model) "")
         :schema (:schema main-body)
         :x-media-type (:x-media-type main-body)
         :x-responses x-responses}
        utils/clean-nils)))

(defmethod to-openapi domain/Parameter [model ctx]
  (debug "Generating parameter " (document/name model))
  (let [base {:description (document/description model)
              :name (or (document/name model) "unnamed")
              :required (domain/required model)
              :in (domain/parameter-kind model)}
        type-info (merge (keywordize-keys (shapes-parser/parse-shape (domain/shape model) ctx)))]
    (-> (merge base type-info)
        utils/clean-nils)))

(defmethod to-openapi domain/Type [model context]
  (debug "Generating type")
  (keywordize-keys (shapes-parser/parse-shape (domain/shape model) context)))

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

(defmethod to-openapi nil [_ _]
  (debug "Generating nil")
  nil)
