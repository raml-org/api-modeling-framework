(ns api-modelling-framework.parser.domain.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.parser.document.jsonld :as json-document]
            [api-modelling-framework.parser.document.jsonld :as json-document-parser]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [log debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                     nil
    (utils/has-class? model v/http:APIDocumentation) v/http:APIDocumentation
    (utils/has-class? model v/document:SourceMap)    v/document:SourceMap
    (utils/has-class? model v/document:Tag)          v/document:Tag
    (utils/has-class? model v/http:EndPoint)         v/http:EndPoint
    (utils/has-class? model v/hydra:Operation)       v/hydra:Operation
    (utils/has-class? model v/http:Response)         v/http:Response
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
  (json-document-parser/from-jsonld m))

(defmethod from-jsonld v/document:Tag [m]
  (json-document-parser/from-jsonld m))

(defmethod from-jsonld v/hydra:Operation [m]
  (debug "Parsing " v/hydra:Operation " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map json-document/from-jsonld sources)]
    (domain/map->ParsedOperation {:id (get m "@id")
                                  :sources parsed-sources
                                  :name (utils/find-value m v/sorg:name)
                                  :description (utils/find-value m v/sorg:description)
                                  :accepts (utils/find-values m v/http:accepts)
                                  :content-type (utils/find-values m v/http:content-type)
                                  :scheme (utils/find-values m v/http:scheme)
                                  :method (utils/find-value m v/hydra:method)
                                  :responses (map from-jsonld (-> m (get v/hydra:returns [])))})))

(defmethod from-jsonld v/http:Response [m]
  (debug "Parsing " v/http:Response " " (get m "@id"))
  (let [sources (get m v/document:source)
        parsed-sources (map json-document/from-jsonld sources)]
    (domain/map->ParsedResponse {:id (get m "@id")
                                 :sources parsed-sources
                                 :name (utils/find-value m v/sorg:name)
                                 :description (utils/find-value m v/sorg:description)
                                 :status-code (utils/find-value m v/hydra:statusCode)})))

(defmethod from-jsonld nil [m]
  (debug "Parsing " nil)
  nil)

(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  nil)
