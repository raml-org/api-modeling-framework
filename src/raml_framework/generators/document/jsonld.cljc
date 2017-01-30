(ns raml-framework.generators.document.jsonld
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.utils :as utils]))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model
    (and (satisfies? document/SourceMap model)
         (satisfies? document/Node model))       :source-map
    (and (satisfies? document/Tag model)
         (satisfies? document/Node model))       :tag
    :else                                        (type model)))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn with-source-maps
  "Adds source maps to a model if required"
  [source-maps? model generated]
  (if source-maps?
    (let [source-maps (document/sources model)]
      (assoc generated v/document:source (map #(to-jsonld % source-maps?) source-maps)))))

(defmethod to-jsonld raml_framework.model.document.Document [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:Document
                 v/document:Unit]
        v/document:declares [(to-jsonld (document/declares m) source-maps?)]
        v/document:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld :source-map [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:SourceMap]
        v/document:location [{"@id" (document/source m)}]
        v/document:tag      (map #(to-jsonld % source-maps?) (document/tags m))}
       (utils/clean-nils)))

(defmethod to-jsonld :tag [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:Tag]
        v/document:tag-id [{"@value" (document/tag-id m)}]
        v/document:tag-value [{"@value" (document/value m)}]}
       (utils/clean-nils)))

(defmethod to-jsonld raml_framework.model.document.Fragment [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:Fragment
                 v/document:Module
                 v/document:Unit]
        v/document:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld nil [_ _ ] nil)
