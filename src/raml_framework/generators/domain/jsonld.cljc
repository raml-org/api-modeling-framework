(ns raml-framework.generators.domain.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [debug]]))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model
    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       :APIDocumentation
    :else                                        (type model)))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn with-node-properties
  "Adds common node properties"
  [node m]
  (-> node
      (assoc "@id" (document/id m))
      (utils/assoc-value m v/sorg:name document/name)
      (utils/assoc-value m v/sorg:description document/description)
      (utils/assoc-objects m v/document:source document/sources to-jsonld)))


(defmethod to-jsonld :APIDocumentation [m source-maps?]
  (debug "Generating APIDocumentation")
  (-> {"@type" [v/http:APIDocumentation
                v/document:DomainElement]}
      (with-node-properties m)
      (utils/assoc-value m v/http:host domain/host)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-value m v/http:base-path domain/base-path)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-object m v/sorg:provider domain/provider to-jsonld)
      (utils/assoc-value m v/http:terms-of-service domain/terms-of-service)
      (utils/assoc-value m v/sorg:version domain/version)
      (utils/assoc-object m v/sorg:license domain/license to-jsonld)
      (utils/clean-nils)))
