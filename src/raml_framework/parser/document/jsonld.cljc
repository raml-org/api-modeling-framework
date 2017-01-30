(ns raml-framework.parser.document.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))


(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                                  nil
    (utils/has-class? model v/document:Document)  v/document:Document
    (utils/has-class? model v/document:Fragment)  v/document:Fragment
    (utils/has-class? model v/document:SourceMap) v/document:SourceMap
    (utils/has-class? model v/document:Tag)       v/document:Tag
    :else                                      :unknown))


(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/document:Document [m]
  (debug "Parsing " v/document:Document)
  (let [encodes (from-jsonld (get m v/document:encodes))
        declares (from-jsonld (get m v/document:declares))
        location (get m "@id")
        source-map (first (map from-jsonld (get m v/document:source [])))
        document-type-tag (utils/find-tag source-map document/document-type-tag)
        document-type (if (some? document-type-tag) (document/value document-type-tag) nil)]
    (document/->Document location encodes declares document-type)))


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
      (reify
        document/Tag
        (document/tag-id [this] tag-id)
        (document/value [this] tag-value)
        document/Node
        (document/id [this] id)
        (document/name [this] (str tag-id " tag"))
        (document/description [this] (str "A " tag-id " tag"))
        (document/valid? [this] true)))))


(defmethod from-jsonld v/document:Fragment [m]
  (debug "Parsing " v/document:Fragment  " " (get m "@id"))
  (let [encodes (from-jsonld (get m v/document:encodes))
        source-map (first (map from-jsonld (get m v/document:source [])))
        file-parsed-tag (when (some? source-map)
                          (->> (document/tags source-map)
                               (filter #(= document/file-parsed-tag
                                           (document/tag-id %)))
                               first))
        location (get m "@id")
        source-map (first (map from-jsonld (get m v/document:source [])))
        document-type-tag (utils/find-tag source-map document/document-type-tag)
        document-type (if (some? document-type-tag) (document/value document-type-tag) nil)]
    (document/->Fragment location encodes document-type)))


(defmethod from-jsonld :unknown [m]
  (debug "Parsing " :unknown)
  (reify document/Node
    (document/id [this] (get m "@id"))
    (document/name [this] "unknown node")
    (document/description [this] (str m))
    (document/valid? [this] true)))

(defmethod from-jsonld nil [_]
  (debug "Parsing " nil)
  nil)
