(ns api-modeling-framework.generators.domain.jsonld
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.platform :as platform]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-jsonld-dispatch-fn [model context]
  (cond
    (nil? model)                                    nil

    (satisfies? domain/DomainPropertySchema model)  :DomainPropertySchema

    (satisfies? domain/DomainProperty model)        :DomainProperty

    (satisfies? document/Extends model)             :Extends

    (satisfies? document/Includes model)            :Includes

    (satisfies? domain/Payload model)               :Payload

    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))          :APIDocumentation

    (and (satisfies? document/SourceMap model)
         (satisfies? document/Node model))          :SourceMap

    (and (satisfies? document/Tag  model)
         (satisfies? document/Node model))          :Tag

    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))          :EndPoint

    (and (satisfies? domain/Operation model)
         (satisfies? document/Node model))          :Operation

    (and (satisfies? domain/Response model)
         (satisfies? document/Node model))          :Response

    (and (satisfies? domain/PayloadHolder model)
         (satisfies? domain/HeadersHolder model)
         (satisfies? domain/ParametersHolder model)
         (satisfies? document/Node model))          :Request

    (and (satisfies? domain/Parameter model)
         (satisfies? document/Node model))          :Parameter

    (and (satisfies? domain/Type model)
         (satisfies? document/Node model))          :Type

    :else                                           (type model)))

(defmulti to-jsonld (fn [model context] (to-jsonld-dispatch-fn model context)))

(defn initial-value [node m {:keys [source-maps?] :as context}]
  (if source-maps?
    (utils/assoc-objects node m v/document:source document/sources (fn [x] (to-jsonld x context)))
    node))

(defn with-node-properties
  "Adds common node properties"
  [node m context]
  (let [node (-> (initial-value node m context)
                 (assoc "@id" (document/id m))
                 (utils/assoc-value m v/sorg:name document/name)
                 (utils/assoc-value m v/sorg:description document/description)
                 (utils/assoc-objects m v/document:additional-properties document/additional-properties (fn [x] (to-jsonld x context))))]
    (if (satisfies? domain/DomainElement m)
      (utils/assoc-value node m v/document:abstract domain/abstract)
      node)))


(defmethod to-jsonld :APIDocumentation [m context]
  (debug "Generating APIDocumentation " (document/id m))
  (-> {"@type" [v/http:APIDocumentation
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-value m v/http:host domain/host)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-objects m v/http:parameter domain/parameters (fn [x] (to-jsonld x context)))
      (utils/assoc-value m v/http:base-path domain/base-path)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-object m v/sorg:provider domain/provider #(to-jsonld % context))
      (utils/assoc-value m v/http:terms-of-service domain/terms-of-service)
      (utils/assoc-value m v/sorg:version domain/version)
      (utils/assoc-object m v/sorg:license domain/license (fn [x] (to-jsonld x context)))
      (utils/assoc-objects m v/http:endpoint domain/endpoints (fn [x] (to-jsonld x context)))
      (utils/clean-nils)))

(defmethod to-jsonld :EndPoint [m context]
  (debug "Generating EndPoint " (document/id m))
  (-> {"@type" [v/http:EndPoint
                v/document:DomainElement]}
       (with-node-properties m context)
       (utils/assoc-value m v/http:path domain/path)
       (utils/assoc-objects m v/hydra:supportedOperation domain/supported-operations (fn [x] (to-jsonld x context)))
       (utils/assoc-objects m v/document:extends document/extends (fn [x] (to-jsonld x context)))
       (utils/assoc-objects m v/http:parameter domain/parameters (fn [x] (to-jsonld x context)))
       utils/clean-nils))

(defmethod to-jsonld :Operation [m context]
  (debug "Generating Operation " (document/id m))
  (-> {"@type" [v/hydra:Operation
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-value m v/hydra:method domain/method)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-objects m v/hydra:returns domain/responses (fn [x] (to-jsonld x context)))
      (utils/assoc-object m v/hydra:expects domain/request (fn [x] (to-jsonld x context)))
      (utils/assoc-objects m v/document:extends document/extends (fn [x] (to-jsonld x context)))
      utils/clean-nils))

(defmethod to-jsonld :Response [m context]
  (debug "Generating Response " (document/id m))
  (-> {"@type" [v/http:Response
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-value m v/hydra:statusCode domain/status-code)
      (utils/assoc-objects m v/http:payload domain/payloads (fn [x] (to-jsonld x context)))
      utils/clean-nils))

(defmethod to-jsonld :Request [m context]
  (debug "Generating Request " (document/id m))
  (let [headers (map #(to-jsonld % context) (or (domain/headers m) []))
        request (-> {"@type" [v/http:Request
                              v/document:DomainElement]}
                    (with-node-properties m context)
                    (utils/assoc-objects m v/http:parameter domain/parameters (fn [x] (to-jsonld x context)))
                    (utils/assoc-objects m v/http:payload domain/payloads (fn [x] (to-jsonld x context)))
                    utils/clean-nils)
        params (get request v/http:parameter [])
        all-params (concat params headers)]
    (if (empty? all-params)
      request
      (assoc request v/http:parameter all-params))))

(defmethod to-jsonld :Parameter [m context]
  (debug "Generating Parameter " (document/id m))
  (-> {"@type" [v/http:Parameter
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-value m v/hydra:required domain/required)
      (utils/assoc-value m v/http:param-binding domain/parameter-kind)
      (utils/assoc-object m v/http:schema domain/shape identity)
      utils/clean-nils))

(defmethod to-jsonld :Payload [m context]
  (debug "Generating Payload " (document/id m))
  (-> {"@type" [v/http:Payload
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-object m v/http:schema domain/schema (fn [x] (to-jsonld x context)))
      (utils/assoc-value m v/http:media-type domain/media-type)
      utils/clean-nils))

(defmethod to-jsonld :Type [m context]
  (debug "Generating Type " (document/id m))
  (let [shape (domain/shape m)]
    (if (nil?  (get shape v/sorg:name))
      (utils/assoc-value shape m v/sorg:name document/name)
      shape)))

(defmethod to-jsonld :Extends [m context]
  (debug "Generating Extends " (document/id m))
  (-> {"@id" (document/id m)
       "@type" [v/document:ExtendRelationship]}
      (with-node-properties m context)
      (utils/assoc-link m v/document:target document/target)
      (utils/assoc-value m v/document:label document/label)
      (assoc v/document:arguments (->> m document/arguments (map (fn [arg] {"@value" (platform/encode-json arg)}))))
      ))

(defmethod to-jsonld :Includes [m context]
  (debug "Generating Includes " (document/id m))
  (-> {"@id" (document/id m)
       "@type" [v/document:IncludeRelationship]}
      (with-node-properties m context)
      (utils/assoc-link m v/document:target document/target)
      (utils/assoc-value m v/document:label document/label)))

(defmethod to-jsonld :SourceMap [m {:keys [source-maps?]}]
  (debug "Generating SourceMap" (document/id m))
  (->> {"@id" (document/id m)
        "@type" [v/document:SourceMap]
        v/document:location [{"@id" (document/source m)}]
        v/document:tag      (map #(to-jsonld % source-maps?) (document/tags m))}
       (utils/clean-nils)))

(defmethod to-jsonld :Tag [m {:keys [source-maps?]}]
  (debug "Generating Tag" (document/id m))
  (->> {"@id" (document/id m)
        "@type" [v/document:Tag]
        v/document:tag-id [{"@value" (document/tag-id m)}]
        v/document:tag-value [{"@value" (document/value m)}]}
       (utils/clean-nils)))

(defmethod to-jsonld :DomainPropertySchema [m context]
  (debug "Generating DomainPropertySchema" (document/id m))
  (-> {"@id" (document/id m)
       "@type" [v/document:DomainPropertySchema]}
      (with-node-properties m context)
      (utils/assoc-objects m v/document:domain domain/domain (fn [node-uri] {"@id" node-uri}))
      (utils/assoc-object m v/document:range domain/range #(to-jsonld % context))
      (utils/clean-nils)))


(defmethod to-jsonld :DomainProperty [m context]
  (debug "Generating DomainProperty" (document/id m))
  (-> {"@id" (document/id m)
       "@type" [v/document:DomainProperty]}
      (with-node-properties m context)
      (utils/assoc-object m v/document:object domain/object identity)
      (utils/clean-nils)))

(defmethod to-jsonld nil [_ _] nil)
