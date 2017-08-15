(ns api-modeling-framework.parser.domain.jsonld
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.utils :as utils]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                              nil
    ;(utils/has-class? model v/document:AbstractDomainElement) v/document:AbstractDomainElement
    (utils/has-class? model v/http:APIDocumentation)          v/http:APIDocumentation
    (utils/has-class? model v/document:DomainPropertySchema)  v/document:DomainPropertySchema
    (utils/has-class? model v/document:DomainProperty)        v/document:DomainProperty
    (utils/has-class? model v/document:Tag)                   v/document:Tag
    (utils/has-class? model v/document:SourceMap)             v/document:SourceMap
    (utils/has-class? model v/document:Tag)                   v/document:Tag
    (utils/has-class? model v/http:Parameter)                 v/http:Parameter
    (utils/has-class? model v/http:EndPoint)                  v/http:EndPoint
    (utils/has-class? model v/hydra:Operation)                v/hydra:Operation
    (utils/has-class? model v/http:Response)                  v/http:Response
    (utils/has-class? model v/http:Request)                   v/http:Request
    (utils/has-class? model v/sh:Shape)                       v/sh:Shape
    (utils/has-class? model (v/shapes-ns "Shape"))            v/sh:Shape
    (utils/has-class? model (v/shapes-ns "NodeShape"))        v/sh:Shape
    (utils/has-class? model v/http:Payload)                   v/http:Payload

    (utils/has-class? model v/document:IncludeRelationship)   v/document:IncludeRelationship
    (utils/has-class? model v/document:ExtendRelationship)    v/document:ExtendRelationship
    :else                                            :unknown))

(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/http:APIDocumentation [m]
  (utils/debug "Parsing " v/http:APIDocumentation " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)
        parameters (utils/map-nodes m v/http:parameter from-jsonld)
        endpoints (utils/map-nodes m v/http:endpoint from-jsonld)]
    (domain/map->ParsedAPIDocumentation {:id (get m "@id")
                                         :sources parsed-sources
                                         :abstract (utils/find-value m v/document:abstract)
                                         :name (utils/find-value m v/sorg:name)
                                         :description (utils/find-value m v/sorg:description)
                                         :host (utils/find-value m v/http:host)
                                         :scheme (utils/find-values m v/http:scheme)
                                         :base-path (utils/find-value m v/http:base-path)
                                         :accepts (utils/find-values m v/http:accepts)
                                         :content-type (utils/find-values m v/http:content-type)
                                         :provider (utils/map-nodes m v/sorg:provider from-jsonld)
                                         :terms-of-service (utils/find-value m v/http:terms-of-service)
                                         :parameters parameters
                                         :version (utils/find-value m v/sorg:version)
                                         :license (utils/map-nodes m v/sorg:license from-jsonld)
                                         :additional-properties additional-properties
                                         :endpoints endpoints})))

(defmethod from-jsonld v/http:EndPoint [m]
  (utils/debug "Parsing " v/http:EndPoint " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        extensions (utils/map-nodes m v/document:extends from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedEndPoint {:id (get m "@id")
                                 :sources parsed-sources
                                 :abstract (utils/find-value m v/document:abstract)
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :path (utils/find-value m v/http:path)
                                 :extends extensions
                                 :additional-properties additional-properties
                                 :parameters (utils/map-nodes m v/http:parameter from-jsonld)
                                 :supported-operations (utils/map-nodes m v/hydra:supportedOperation from-jsonld)})))

(defmethod from-jsonld v/document:SourceMap [m]
  (utils/debug "Parsing " v/document:SourceMap " " (get m "@id"))
  (let [id (get m "@id")
        location (-> m (get v/document:location) first (get "@id"))
        tags (map from-jsonld (get m v/document:tag []))]
    (document/->DocumentSourceMap id location tags [])))


(defmethod from-jsonld v/document:Tag [m]
  (utils/debug "Parsing " v/document:Tag  " " (get m "@id"))
  (let [id (get m "@id")
        tag-id (utils/find-value m v/document:tag-id)
        tag-value (utils/find-value m v/document:tag-value)]
    (condp = tag-id
      document/file-parsed-tag (document/->FileParsedTag id tag-value)
      document/document-type-tag (document/->DocumentTypeTag id tag-value)
      document/node-parsed-tag (document/->NodeParsedTag id tag-value)
      document/nested-resource-children-tag (document/->NestedResourceChildrenTag id tag-value)
      document/nested-resource-parent-id-tag (document/->NestedResourceParentIdTag id tag-value)
      document/nested-resource-path-parsed-tag (document/->NestedResourcePathParsedTag id tag-value)
      document/api-tag-tag (document/->APITagTag id tag-value)
      document/inline-fragment-parsed-tag (document/->InlineFragmentParsedTag id tag-value)
      (reify
        document/Tag
        (document/tag-id [this] tag-id)
        (document/value [this] tag-value)
        document/Node
        (document/id [this] id)
        (document/name [this] (str tag-id " tag"))
        (document/description [this] (str "A " tag-id " tag"))
        (document/valid? [this] true)))))


(defmethod from-jsonld v/hydra:Operation [m]
  (utils/debug "Parsing " v/hydra:Operation " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        extensions (utils/map-nodes m v/document:extends from-jsonld)
        request (first (utils/map-nodes m v/hydra:expects from-jsonld))
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedOperation {:id (get m "@id")
                                  :sources parsed-sources
                                  :abstract (utils/find-value m v/document:abstract)
                                  :name (utils/find-value m v/sorg:name)
                                  :description (utils/find-value m v/sorg:description)
                                  :accepts (utils/find-values m v/http:accepts)
                                  :content-type (utils/find-values m v/http:content-type)
                                  :scheme (utils/find-values m v/http:scheme)
                                  :method (utils/find-value m v/hydra:method)
                                  :extends extensions
                                  :additional-properties additional-properties
                                  :request request
                                  :responses (utils/map-nodes m v/hydra:returns from-jsonld)})))

(defmethod from-jsonld v/http:Response [m]
  (utils/debug "Parsing " v/http:Response " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedResponse {:id (get m "@id")
                                 :sources parsed-sources
                                 :abstract (utils/find-value m v/document:abstract)
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :additional-properties additional-properties
                                 :payloads (utils/map-nodes m v/http:payload from-jsonld)
                                 :status-code (utils/find-value m v/hydra:statusCode)})))

(defmethod from-jsonld v/http:Payload [m]
  (utils/debug "Parsing " v/http:Payload " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedPayload {:id (get m "@id")
                                :sources parsed-sources
                                :abstract (utils/find-value m v/document:abstract)
                                :name (utils/find-value m v/sorg:name)
                                :description (utils/find-value m v/sorg:description)
                                :media-type (utils/find-value m v/http:media-type)
                                :additional-properties additional-properties
                                :schema (from-jsonld (first (get m v/http:schema)))})))

(defmethod from-jsonld v/sh:Shape [m]
  (utils/debug "Parsing " v/sh:Shape " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedType {:id (str (get m "@id") "/wrapper")
                             :abstract (utils/find-value m v/document:abstract)
                             :name (utils/find-value m v/sorg:name)
                             :additional-properties additional-properties
                             ;; shapes are expressed already in JSON-LD, they are passed as it
                             :shape m})))

(defmethod from-jsonld v/http:Parameter [m]
  (utils/debug "Parsing " v/http:Parameter " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (domain/map->ParsedParameter {:id (get m "@id")
                                  :sources parsed-sources
                                  :abstract (utils/find-value m v/document:abstract)
                                  :name (utils/find-value m v/sorg:name)
                                  :description (utils/find-value m v/sorg:description)
                                  :additional-properties additional-properties
                                  ;; shapes are expressed already in JSON-LD, they are passed as it
                                  :required (utils/find-value m v/hydra:required)
                                  :parameter-kind (utils/find-value m v/http:param-binding)
                                  :shape (utils/extract-jsonld m v/http:schema)})))

(defmethod from-jsonld v/http:Request [m]
  (utils/debug "Parsing " v/http:Request " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)
        params (utils/find-nodes m v/http:parameter)
        headers (->> params
                     (filter #(= "header" (utils/find-value % v/http:param-binding)))
                     (map #(from-jsonld %)))
        not-headers (->> params
                         (filter #(not= "header" (utils/find-value % v/http:param-binding)))
                         (map #(from-jsonld %)))]
    (domain/map->ParsedRequest {:id (get m "@id")
                                :sources parsed-sources
                                :abstract (utils/find-value m v/document:abstract)
                                :name (utils/find-value m v/sorg:name)
                                :description (utils/find-value m v/sorg:description)
                                :headers headers
                                :parameters not-headers
                                :additional-properties additional-properties
                                :payloads (utils/map-nodes m v/http:payload from-jsonld)})))

(defmethod from-jsonld v/document:ExtendRelationship [m]
  (utils/debug "Parsing " v/document:ExtendRelationship " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (document/map->ParsedExtends {:id (get m "@id")
                                  :sources parsed-sources
                                  :target (utils/find-link m v/document:target)
                                  :label (utils/find-value m v/document:label)
                                  :name (utils/find-value m v/sorg:name)
                                  :additional-properties additional-properties
                                  :arguments (utils/find-values m v/document:arguments)})))

(defmethod from-jsonld v/document:IncludeRelationship [m]
  (utils/debug "Parsing " v/document:IncludeRelationship " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)
        additional-properties (utils/map-nodes m v/document:additional-properties from-jsonld)]
    (document/map->ParsedExtends {:id (get m "@id")
                                  :sources parsed-sources
                                  :target (utils/find-link m v/document:target)
                                  :label (utils/find-value m v/document:label)
                                  :name (utils/find-value m v/sorg:name)})))

(defmethod from-jsonld v/document:DomainPropertySchema [m]
  (utils/debug "Parsing " v/document:DomainPropertySchema  " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)]
    (domain/map->ParsedDomainPropertySchema {:id (get m "@id")
                                             :name (utils/find-value m v/sorg:name)
                                             :description (utils/find-value m v/sorg:description)
                                             :sources parsed-sources
                                             :domain (utils/find-values m v/document:domain)
                                             :range (first (utils/map-nodes m v/document:range from-jsonld))})))

(defmethod from-jsonld v/document:DomainProperty [m]
  (utils/debug "Parsing " v/document:DomainProperty  " " (get m "@id"))
  (let [parsed-sources (utils/map-nodes m v/document:source from-jsonld)]
    (domain/map->ParsedDomainProperty {:id (get m "@id")
                                       :name (utils/find-value m v/sorg:name)
                                       :description (utils/find-value m v/sorg:description)
                                       :sources parsed-sources
                                       :predicate (utils/find-node m v/document:predicate)
                                       :object (utils/find-node m v/document:object)})))

(defmethod from-jsonld nil [m]
  (utils/debug "Parsing " nil)
  nil)

(defmethod from-jsonld :unknown [m]
  (utils/debug "Parsing " :unknown)
  nil)
