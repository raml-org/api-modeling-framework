(ns raml-framework.generators.domain.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.generators.document.jsonld :as jsonld-document-generator]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model

    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       :APIDocumentation

    (and (satisfies? document/SourceMap model)
         (satisfies? document/Node model))       :SourceMap

    (and (satisfies? document/Tag  model)
         (satisfies? document/Node model))       :Tag

    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))       :EndPoint

    (and (satisfies? domain/Operation model)
         (satisfies? document/Node model))       :Operation

    (and (satisfies? domain/Response model)
         (satisfies? document/Node model))       :Response

    :else                                        (type model)))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn with-node-properties
  "Adds common node properties"
  [node m source-maps?]
  (-> node
      (assoc "@id" (document/id m))
      (utils/assoc-value m v/sorg:name document/name)
      (utils/assoc-value m v/sorg:description document/description)
      (utils/assoc-objects m v/document:source document/sources (fn [x] (to-jsonld x source-maps?)))))


(defmethod to-jsonld :APIDocumentation [m source-maps?]
  (debug "Generating APIDocumentation " (document/id m))
  (-> {"@type" [v/http:APIDocumentation
                v/document:DomainElement]}
      (with-node-properties m source-maps?)
      (utils/assoc-value m v/http:host domain/host)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-value m v/http:base-path domain/base-path)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-object m v/sorg:provider domain/provider to-jsonld)
      (utils/assoc-value m v/http:terms-of-service domain/terms-of-service)
      (utils/assoc-value m v/sorg:version domain/version)
      (utils/assoc-object m v/sorg:license domain/license (fn [x] (to-jsonld x source-maps?)))
      (utils/assoc-objects m v/http:endpoint domain/endpoints (fn [x] (to-jsonld x source-maps?)))
      (utils/clean-nils)))

(defmethod to-jsonld :Tag [m source-maps?]
  (jsonld-document-generator/to-jsonld m source-maps?))


(defmethod to-jsonld :SourceMap [m source-maps?]
  (jsonld-document-generator/to-jsonld m source-maps?))

(defmethod to-jsonld :EndPoint [m source-maps?]
  (debug "Generating EndPoint " (document/id m))
  (-> {"@type" [v/http:EndPoint
                v/document:DomainElement]}
       (with-node-properties m source-maps?)
       (utils/assoc-value m v/http:path domain/path)
       (utils/assoc-objects m v/hydra:supportedOperation domain/supported-operations (fn [x] (to-jsonld x source-maps?)))
       utils/clean-nils))

(defmethod to-jsonld :Operation [m source-maps?]
  (debug "Generating Operation " (document/id m))
  (-> {"@type" [v/hydra:Operation
                v/document:DomainElement]}
      (with-node-properties m source-maps?)
      (utils/assoc-value m v/hydra:method domain/method)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-objects m v/hydra:returns domain/responses (fn [x] (to-jsonld x source-maps?)))
      utils/clean-nils))

(defmethod to-jsonld :Response [m source-maps?]
  (debug "Generating Response " (document/id m))
  (-> {"@type" [v/http:Response
                v/document:DomainElement]}
      (with-node-properties m source-maps?)
      (utils/assoc-value m v/hydra:statusCode domain/status-code)
      utils/clean-nils))
