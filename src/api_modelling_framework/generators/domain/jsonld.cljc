(ns api-modelling-framework.generators.domain.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.platform :as platform]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-jsonld-dispatch-fn [model context]
  (cond
    (nil? model)                                    nil

    (satisfies? domain/DomainElement model)         :DomainElement

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
  (-> (initial-value node m context)
      (assoc "@id" (document/id m))
      (utils/assoc-value m v/sorg:name document/name)
      (utils/assoc-value m v/sorg:description document/description)))


(defmethod to-jsonld :APIDocumentation [m context]
  (debug "Generating APIDocumentation " (document/id m))
  (-> {"@type" [v/http:APIDocumentation
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-value m v/http:host domain/host)
      (utils/assoc-values m v/http:scheme domain/scheme)
      (utils/assoc-value m v/http:base-path domain/base-path)
      (utils/assoc-values m v/http:accepts domain/accepts)
      (utils/assoc-values m v/http:content-type domain/content-type)
      (utils/assoc-object m v/sorg:provider domain/provider to-jsonld)
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
      (utils/assoc-object m v/http:shape domain/shape identity)
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
  (-> {"@type" [v/http:Schema
                v/document:DomainElement]}
      (with-node-properties m context)
      (utils/assoc-object m v/http:shape domain/shape identity)
      utils/clean-nils))

;; Abstract domain elements are slightly different, we add the properties from
;; Domain element, like the fragment node name, and then we merge the set of properties
;; coming from the encoded element, that can be incomplete
(defmethod to-jsonld :DomainElement [m context]
  (debug "Generating DomainElement " (document/id m))
  (let [domain-element-types  [v/document:DomainElement
                               v/document:AbstractDomainElement]
        domain-element-properties (-> {}
                                      (with-node-properties m context)
                                      (utils/assoc-value m v/document:fragment-node domain/fragment-node)
                                      utils/clean-nils)
        encoded-element (to-jsonld (domain/to-domain-node m) context)
        encoded-element-types (flatten [(get encoded-element "@type" [])])
        encoded-element (assoc encoded-element "@type" (distinct (concat domain-element-types encoded-element-types)))]
    (merge domain-element-properties encoded-element)))

(defmethod to-jsonld :Extends [m context]
  (debug "Generating Extends " (document/id m))
  (-> {"@type" [v/document:ExtendRelationship]}
      (with-node-properties m context)
      (utils/assoc-link m v/document:target document/id)
      (utils/assoc-value m v/document:label document/label)
      (assoc v/document:arguments (->> m document/arguments (map (fn [arg] {"@value" (platform/encode-json arg)}))))
      ))

(defmethod to-jsonld :Includes [m context]
  (debug "Generating Includes " (document/id m))
  (-> {"@type" [v/document:IncludeRelationship]}
      (with-node-properties m context)
      (utils/assoc-link m v/document:target document/id)
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

(defmethod to-jsonld nil [_ _] nil)
