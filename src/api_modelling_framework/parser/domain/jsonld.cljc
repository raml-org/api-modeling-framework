(ns api-modelling-framework.parser.domain.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.parser.document.jsonld :as json-document]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [log debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                           nil
    (utils/has-class? model v/http:APIDocumentation)       v/http:APIDocumentation
    (utils/has-class? model v/document:SourceMap)          v/document:SourceMap
    (utils/has-class? model v/document:Tag)                v/document:Tag
    (utils/has-class? model v/http:EndPoint)               v/http:EndPoint
    (utils/has-class? model v/hydra:Operation)             v/hydra:Operation
    (utils/has-class? model v/http:Response)               v/http:Response
    (utils/has-class? model v/http:Request)                v/http:Request
    (utils/has-class? model v/http:Payload)                v/http:Payload
    (utils/has-class? model v/http:Parameter)              v/http:Parameter
    (utils/has-class? model v/document:ExtendRelationship) v/document:ExtendRelationship
    :else                                            :unknown))

(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/http:APIDocumentation [m]
  (debug "Parsing " v/http:APIDocumentation " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map json-document/from-jsonld sources)]
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
        parsed-sources (map json-document/from-jsonld sources)]
    (domain/map->ParsedEndPoint {:id (get m "@id")
                                 :sources parsed-sources
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :path (utils/find-value m v/http:path)
                                 :supported-operations (map from-jsonld (-> m (get v/hydra:supportedOperation [])))})))

(defmethod from-jsonld v/document:SourceMap [m]
  (json-document/from-jsonld m))

(defmethod from-jsonld v/document:Tag [m]
  (json-document/from-jsonld m))

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
        parsed-sources (map json-document/from-jsonld sources)
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
        parsed-sources (map json-document/from-jsonld sources)]
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
        parsed-sources (map json-document/from-jsonld sources)]
    (domain/map->ParsedType {:id (get m "@id")
                             :sources parsed-sources
                             :name (utils/find-value m v/sorg:name)
                             :description (utils/find-value m v/sorg:description)
                             ;; shapes are expressed already in JSON-LD, they are passed as it
                             :shape (utils/extract-jsonld m v/http:shape)})))

(defmethod from-jsonld v/http:Parameter [m]
  (debug "Parsing " v/http:Parameter " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map json-document/from-jsonld sources)]
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
        parsed-sources (map json-document/from-jsonld sources)]
    (domain/map->ParsedRequest {:id (get m "@id")
                                :sources parsed-sources
                                :name (utils/find-value m v/sorg:name)
                                :description (utils/find-value m v/sorg:description)
                                :parameters (map #(from-jsonld %) (get m v/http:parameter []))
                                :schema (from-jsonld (-> m (get v/http:payload) first))})))

(defmethod from-jsonld v/document:ExtendRelationship [m]
  (debug "Parsing " v/document:ExtendRelationship " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map json-document/from-jsonld sources)]
    (document/map->ParsedExtends {:id (get m "@id")
                                  :sources parsed-sources
                                  :target (utils/find-link m v/document:target)
                                  :label (utils/find-value m v/document:label)
                                  :name (utils/find-value m v/sorg:name)
                                  :arguments (utils/find-values m v/document:arguments)})))

(defmethod from-jsonld nil [m]
  (debug "Parsing " nil)
  nil)

(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  nil)
