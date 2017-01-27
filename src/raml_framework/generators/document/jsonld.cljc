(ns raml-framework.generators.document.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model
    (and (satisfies? document/SourceMap model)
         (satisfies? document/Node model))       :source-map
    (and (satisfies? document/Tag model)
         (satisfies? document/Node model))       :tag
    :else                                        (type model)))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn clean-nils [jsonld]
  (->> jsonld
       (map (fn [[k v]]
              (let [v (if (coll? v) (filter some? v) v)]
                (cond
                  (nil? v)                   nil
                  (and (coll? v) (empty? v)) nil
                  (and (map? v) (= v {}))    nil
                  :else                      [k v]))))
       (filter some?)
       (into {})))

(defn with-source-maps
  "Adds source maps to a model if required"
  [source-maps? model generated]
  (if source-maps?
    (let [source-maps (document/sources model)]
      (assoc generated v/model:source (map #(to-jsonld % source-maps?) source-maps)))))


(defmethod to-jsonld raml_framework.model.document.Document [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/model:Document
                 v/model:Unit]
        v/model:declares [(to-jsonld (document/declares m) source-maps?)]
        v/model:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (clean-nils)))

(defmethod to-jsonld :source-map [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/model:SourceMap]
        v/model:location [{"@id" (document/location m)}]
        v/model:tag      (map #(to-jsonld % source-maps?) (document/tags m))}
       (clean-nils)))

(defmethod to-jsonld :tag [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/model:Tag]
        v/model:tag-id [{"@value" (document/tag-id m)}]
        v/model:tag-value [{"@value" (document/value m)}]}
       (clean-nils)))

(defmethod to-jsonld raml_framework.model.document.Fragment [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/model:Fragment
                 v/model:Module
                 v/model:Unit]
        v/model:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (clean-nils)))

(defmethod to-jsonld nil [_ _ ] nil)
