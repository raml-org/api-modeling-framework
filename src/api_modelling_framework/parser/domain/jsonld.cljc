(ns api-modelling-framework.parser.domain.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [log debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                              nil
    (utils/has-class? model v/document:AbstractDomainElement) v/document:AbstractDomainElement
    (utils/has-class? model v/http:APIDocumentation)          v/http:APIDocumentation
    (utils/has-class? model v/document:SourceMap)             v/document:SourceMap
    (utils/has-class? model v/document:Tag)                   v/document:Tag
    (utils/has-class? model v/http:EndPoint)                  v/http:EndPoint
    (utils/has-class? model v/hydra:Operation)                v/hydra:Operation
    (utils/has-class? model v/http:Response)                  v/http:Response
    (utils/has-class? model v/http:Request)                   v/http:Request
    (utils/has-class? model v/http:Payload)                   v/http:Payload
    (utils/has-class? model v/http:Parameter)                 v/http:Parameter
    (utils/has-class? model v/document:IncludeRelationship)   v/document:IncludeRelationship
    (utils/has-class? model v/document:ExtendRelationship)    v/document:ExtendRelationship
    :else                                            :unknown))

(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/http:APIDocumentation [m]
  (debug "Parsing " v/http:APIDocumentation " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (domain/map->ParsedAPIDocumentation {:id (get m "@id")
                                         :sources parsed-sources
                                         :name (utils/find-value m v/sorg:name)
                                         :description (utils/find-value m v/sorg:description)
                                         :host (utils/find-value m v/http:host)
                                         :scheme (utils/find-values m v/http:scheme)
                                         :base-path (utils/find-value m v/http:base-path)
                                         :accepts (utils/find-values m v/http:accepts)
                                         :content-type (utils/find-values m v/http:content-type)
                                         :provider (from-jsonld (-> m (get v/sorg:provider) first))
                                         :terms-of-service (utils/find-value m v/http:terms-of-service)
                                         :version (utils/find-value m v/sorg:version)
                                         :license (from-jsonld (-> m (get v/sorg:license) first))
                                         :endpoints (map from-jsonld (-> m (get v/http:endpoint [])))})))

(defmethod from-jsonld v/http:EndPoint [m]
  (debug "Parsing " v/http:EndPoint " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)
        extend-rels (get m v/document:extends [])
        extensions (map from-jsonld extend-rels)]
    (domain/map->ParsedEndPoint {:id (get m "@id")
                                 :sources parsed-sources
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :path (utils/find-value m v/http:path)
                                 :extends extensions
                                 :supported-operations (map from-jsonld (-> m (get v/hydra:supportedOperation [])))})))

(defmethod from-jsonld v/document:SourceMap [m]
  (debug "Parsing " v/document:SourceMap " " (get m "@id"))
  (let [id (get m "@id")
        location (-> m (get v/document:location) first (get "@id"))
        tags (map from-jsonld (get m v/document:tag []))]
    (document/->DocumentSourceMap  id location tags)))


(defmethod from-jsonld v/document:Tag [m]
  (debug "Parsing " v/document:Tag  " " (get m "@id"))
  (let [id (get m "@id")
        tag-id (-> m (get v/document:tag-id) first (get "@value"))
        tag-value (-> m (get v/document:tag-value) first (get "@value"))]
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


(defn parse-request [request]
  (if (nil? request) {:request nil :headers nil}
      (let [params (get request v/http:parameter [])
            headers (->> params
                         (filter #(= "header" (utils/find-value % v/http:param-binding)))
                         (map #(from-jsonld %)))
            not-headers (filter #(not= "header" (utils/find-value % v/http:param-binding)) params)]
        {:headers headers
         :request (from-jsonld (assoc request v/http:parameter not-headers))})))

(defmethod from-jsonld v/hydra:Operation [m]
  (debug "Parsing " v/hydra:Operation " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)
        extend-rels (get m v/document:extends [])
        extensions (map from-jsonld extend-rels)
        {:keys [request headers]} (parse-request (-> m (get v/hydra:expects) first))]
    (domain/map->ParsedOperation {:id (get m "@id")
                                  :sources parsed-sources
                                  :name (utils/find-value m v/sorg:name)
                                  :description (utils/find-value m v/sorg:description)
                                  :accepts (utils/find-values m v/http:accepts)
                                  :content-type (utils/find-values m v/http:content-type)
                                  :scheme (utils/find-values m v/http:scheme)
                                  :method (utils/find-value m v/hydra:method)
                                  :headers headers
                                  :request request
                                  :extends extensions
                                  :responses (map from-jsonld (-> m (get v/hydra:returns [])))})))

(defmethod from-jsonld v/http:Response [m]
  (debug "Parsing " v/http:Response " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (domain/map->ParsedResponse {:id (get m "@id")
                                 :sources parsed-sources
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :accepts (utils/find-values m v/http:accepts)
                                 :content-type (utils/find-values m v/http:content-type)
                                 :schema (from-jsonld (first (get m v/http:payload)))
                                 :status-code (utils/find-value m v/hydra:statusCode)})))

(defmethod from-jsonld v/http:Payload [m]
  (debug "Parsing " v/http:Payload " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (domain/map->ParsedType {:id (get m "@id")
                             :sources parsed-sources
                             :name (utils/find-value m v/sorg:name)
                             :description (utils/find-value m v/sorg:description)
                             ;; shapes are expressed already in JSON-LD, they are passed as it
                             :shape (utils/extract-jsonld m v/http:shape)})))

(defmethod from-jsonld v/http:Parameter [m]
  (debug "Parsing " v/http:Parameter " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (domain/map->ParsedParameter {:id (get m "@id")
                                  :sources parsed-sources
                                  :name (utils/find-value m v/sorg:name)
                                  :description (utils/find-value m v/sorg:description)
                                  ;; shapes are expressed already in JSON-LD, they are passed as it
                                  :required (utils/find-value m v/hydra:required)
                                  :parameter-kind (utils/find-value m v/http:param-binding)
                                  :shape (utils/extract-jsonld m v/http:shape)})))

(defmethod from-jsonld v/http:Request [m]
  (debug "Parsing " v/http:Request " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (domain/map->ParsedRequest {:id (get m "@id")
                                :sources parsed-sources
                                :name (utils/find-value m v/sorg:name)
                                :description (utils/find-value m v/sorg:description)
                                :parameters (map #(from-jsonld %) (get m v/http:parameter []))
                                :schema (from-jsonld (-> m (get v/http:payload) first))})))

(defmethod from-jsonld v/document:ExtendRelationship [m]
  (debug "Parsing " v/document:ExtendRelationship " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (document/map->ParsedExtends {:id (get m "@id")
                                  :sources parsed-sources
                                  :target (utils/find-link m v/document:target)
                                  :label (utils/find-value m v/document:label)
                                  :name (utils/find-value m v/sorg:name)
                                  :arguments (utils/find-values m v/document:arguments)})))

(defmethod from-jsonld v/document:IncludeRelationship [m]
  (debug "Parsing " v/document:IncludeRelationship " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map from-jsonld sources)]
    (document/map->ParsedExtends {:id (get m "@id")
                                  :sources parsed-sources
                                  :target (utils/find-link m v/document:target)
                                  :label (utils/find-value m v/document:label)
                                  :name (utils/find-value m v/sorg:name)})))

;; Abstract domain elements are slightly different, we have one property coming
;; from the abstract domain element and the remaining properties come from the
;; encoded element, we need to transform those properties in the JSON-LD object
;; into the properties attribute of the domain model
(defmethod from-jsonld v/document:AbstractDomainElement [m]
  (debug "Parsing " v/document:AbstractDomainElement " " (get m "@id"))
  (let [domain-element-types (flatten [(get m "@type" [])])
        domain-element-types (filterv #(not= % v/document:AbstractDomainElement) domain-element-types)
        parsed-encoded (from-jsonld (assoc m "@type" domain-element-types))
        properties (->> parsed-encoded (into {}))]
    (domain/map->ParsedDomainElement {:id (get m "@id")
                                      :properties properties
                                      :fragment-node (keyword (utils/find-value m v/document:fragment-node))})))

(defmethod from-jsonld nil [m]
  (debug "Parsing " nil)
  nil)

(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  nil)
