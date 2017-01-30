(ns raml-framework.generators.domain.openapi
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [debug]]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model)                                 model
    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       domain/APIDocumentation
    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))       domain/EndPoint
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
  ;; temporary until we add the operations logic
  {})

(defmethod to-openapi nil [_ _]
  (debug "Generating nil")
  nil)
