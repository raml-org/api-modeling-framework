(ns raml-framework.parser.domain.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.parser.document.jsonld :as json-document]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [log debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                     nil
    (utils/has-class? model v/http:APIDocumentation) v/http:APIDocumentation
    :else                                            :unknown))

(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/http:APIDocumentation [m]
  (debug "Parsing " v/http:APIDocumentation)
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
                                         :license (from-jsonld (-> m (get v/sorg:license) first))})))


(defmethod from-jsonld nil [m]
  (debug "Parsing " nil)
  nil)

(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  nil)
