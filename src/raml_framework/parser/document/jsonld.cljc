(ns raml-framework.parser.document.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]))

(defn has-class? [m c]
  (-> m
      (get "@type")
      (->> (some #(= % c)))))

(defn from-jsonld-dispatch-fn [model]
  (cond
    (nil? model)                         nil
    (has-class? model v/model:Document)  v/model:Document
    (has-class? model v/model:SourceMap) v/model:SourceMap
    (has-class? model v/model:Tag)       v/model:Tag
    :else                                :unknown))


(defmulti from-jsonld (fn [m] (from-jsonld-dispatch-fn m)))


(defmethod from-jsonld v/model:Document [m]
  (let [encodes (from-jsonld (get m v/model:encodes))
        declares (from-jsonld (get m v/model:declares))
        source-map (first (map from-jsonld (get m v/model:source [])))
        file-parsed-tag (when (some? source-map)
                          (->> (document/tags source-map)
                               (filter #(= document/file-parsed-tag
                                           (document/tag-id %)))
                               first))
        location (get m "@id")
        document-type-tag (when (some? source-map)
                            (->> (document/tags source-map)
                                 (filter #(= document/document-type-tag
                                             (document/tag-id %)))
                                 first))
        document-type (if (some? document-type-tag) (document/value document-type-tag) nil)]
    (document/->Document location encodes declares document-type)))


(defmethod from-jsonld v/model:SourceMap [m]
  (let [id (get m "@id")
        location (-> m (get v/model:location) first (get "@id"))
        tags (map from-jsonld (get m v/model:tag []))]
    (document/->DocumentSourceMap  id location tags)))


(defmethod from-jsonld v/model:Tag [m]
  (let [id (get m "@id")
        tag-id (-> m (get v/model:tag-id) first (get "@value"))
        tag-value (-> m (get v/model:tag-value) first (get "@value"))]
    (condp = tag-id
      document/file-parsed-tag (document/->FileParsedTag id tag-value)
      document/document-type-tag (document/->DocumentTypeTag id tag-value)
      :else (reify
              document/Tag
              (document/tag-id [this] tag-id)
              (document/value [this] tag-value)
              document/Node
              (document/id [this] id)
              (document/name [this] (str tag-id " tag"))
              (document/description [this] (str "A " tag-id " tag"))
              (document/to-jsonld [this source-maps?] m)
              (document/valid? [this] true)))))


(defmethod from-jsonld :unknown [m]
  (reify document/Node
    (document/id [this] (get m "@id"))
    (document/name [this] "unknown node")
    (document/description [this] (str m))
    (document/to-jsonld [this source-maps?] m)
    (document/valid? [this] true)))

(defmethod from-jsonld nil [_] nil)
