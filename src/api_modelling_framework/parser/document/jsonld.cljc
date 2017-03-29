(ns api-modelling-framework.parser.document.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.parser.domain.jsonld :as domain-parser]
            [api-modelling-framework.parser.document.common :refer [get-one]]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                  nil
    (utils/has-class? model v/document:Document)  v/document:Document
    (utils/has-class? model v/document:Fragment)  v/document:Fragment
    (and (coll? model) (= 1 (count model)))       (from-jsonld-dispatch-fn (first model))
    :else                                      :unknown))


(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/document:Document [m]
  (debug "Parsing " v/document:Document)
  (let [encodes (domain-parser/from-jsonld (get-one m v/document:encodes))
        declares (map domain-parser/from-jsonld (get m v/document:declares []))
        references (mapv from-jsonld (get m v/document:references []))
        location (get m "@id")
        source-map (first (map domain-parser/from-jsonld (get m v/document:source [])))
        document-type-tag (utils/find-tag source-map document/document-type-tag)
        document-type (if (some? document-type-tag) (document/value document-type-tag) nil)]
    (document/map->ParsedDocument {:id location
                                   :location location
                                   :encodes encodes
                                   :references references
                                   :declares declares
                                   :document-type document-type})))

(defmethod from-jsonld v/document:Fragment [m]
  (debug "Parsing " v/document:Fragment  " " (get m "@id"))
  (let [encodes (first (mapv domain-parser/from-jsonld (get m v/document:encodes)))
        source-map (first (map domain-parser/from-jsonld (get m v/document:source [])))
        file-parsed-tag (when (some? source-map)
                          (->> (document/tags source-map)
                               (filter #(= document/file-parsed-tag
                                           (document/tag-id %)))
                               first))
        location (get m "@id")
        source-map (first (map domain-parser/from-jsonld (get m v/document:source [])))
        document-type-tag (utils/find-tag source-map document/document-type-tag)
        document-type (if (some? document-type-tag) (document/value document-type-tag) nil)]
    (document/map->ParsedFragment {:id location
                                   :location location
                                   :encodes encodes
                                   :document-type document-type})))


(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  (domain-parser/from-jsonld m))

(defmethod from-jsonld nil [_]
  (debug "Parsing " nil)
  nil)
