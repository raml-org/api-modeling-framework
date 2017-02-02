(ns api-modelling-framework.generators.domain.openapi
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.generators.domain.shapes-json-schema :as shapes-parser]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [clojure.walk :refer [keywordize-keys]]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model)                                 model

    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       domain/APIDocumentation

    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))       domain/EndPoint

    (and (satisfies? domain/Operation model)
         (satisfies? document/Node model))       domain/Operation

    (and (satisfies? domain/Response model)
         (satisfies? document/Node model))       domain/Response

    (and (satisfies? domain/Type model)
         (satisfies? document/Node model))       domain/Type

    :else                                        (type model)))

(defmulti to-openapi (fn [model ctx] (to-openapi-dispatch-fn model ctx)))


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
                           (to-openapi endpoint ctx)]))
                   (into {}))]
    (-> {:host (domain/host model)
         :scheme (domain/scheme model)
         :basePath (domain/base-path model)
         :produces (if (= 1 (count (domain/content-type model)))
                     (first (domain/content-type model))
                     (domain/content-type model))
         :info info
         :consumes (if (= 1 (count (domain/accepts model)))
                     (first (domain/accepts model))
                     (domain/accepts model))
         :paths paths}
        utils/clean-nils)))

(defmethod to-openapi domain/EndPoint [model ctx]
  (debug "Generating resource " (document/id model))
  (let [operations (domain/supported-operations model)]
    (->> operations
         (map (fn [op] [(keyword (domain/method op)) (to-openapi op ctx)]))
         (into {}))))

(defmethod to-openapi domain/Operation [model ctx]
  (debug "Generating operation " (document/id model))
  (let [tags (->> (document/find-tag model document/api-tag-tag)
                  (map #(document/value %)))
        produces(domain/content-type model)
        responses-produces (->> (or (domain/responses model) [])
                                (map #(or (domain/content-type %) []))
                                flatten
                                (filter some?))
        response-bodies-with-media-types (or (not (empty? responses-produces)) nil)]
    (-> {:operationId (document/name model)
         :description (document/description model)
         :tags tags
         :x-response-bodies-with-media-types response-bodies-with-media-types
         :schemes (domain/scheme model)
         :consumes (domain/accepts model)
         :produces (concat produces responses-produces)
         :responses (->> (domain/responses model)
                         (map (fn [response] [(document/name response) response]))
                         ;; we need to avoid multiple responses with the same key
                         ;; this is not allowed in OpenAPI, we deal with this generating an altered key
                         ;; for the duplicated responses.
                         ;; the x-response-bodies-with-media-types guards against this condition

                         ;; first we group
                         (reduce (fn [acc [k v]]
                                   (let [vs (get acc k [])
                                         vs (concat vs [v])]
                                     (assoc acc k vs)))
                                 {})

                         ;; now e generated the keys
                         (map (fn [[k vs]]
                                (if (> (count vs) 1)
                                  (map (fn [i response]
                                         (let [v (to-openapi response ctx)]
                                           [(str k "--" (or (-> response domain/content-type first) i))
                                            v]))
                                       (range 0 (count vs))
                                       vs)
                                  [k (to-openapi (first vs) ctx)])))
                         ;; we recreate the hash
                         flatten
                         (partition 2)
                         (map #(into [] %))
                         (into [])
                         (into {}))}
        utils/clean-nils)))

(defmethod to-openapi domain/Response [model ctx]
  (debug "Generating response " (document/name model))
  (-> {:description (document/description model)
       :schema (to-openapi (domain/schema model) ctx)}
      utils/clean-nils))

(defmethod to-openapi domain/Type [model context]
  (debug "Generating type")
  (keywordize-keys (shapes-parser/parse-shape (domain/shape model) context)))

(defmethod to-openapi nil [_ _]
  (debug "Generating nil")
  nil)
