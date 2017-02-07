(ns api-modelling-framework.generators.document.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model
    (and (satisfies? document/SourceMap model)
         (satisfies? document/Node model))       :source-map
    (and (satisfies? document/Tag model)
         (satisfies? document/Node model))       :tag

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))     :document

    (satisfies? document/Fragment model)         :fragment

    (satisfies? document/Module model)           :library

    :else                                        (type model)))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn with-source-maps
  "Adds source maps to a model if required"
  [source-maps? model generated]
  (if source-maps?
    (let [source-maps (document/sources model)]
      (assoc generated v/document:source (map #(to-jsonld % source-maps?) source-maps)))))

(defmethod to-jsonld :document [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:Document
                 v/document:Fragment
                 v/document:Module
                 v/document:Unit]
        v/document:declares (mapv #(to-jsonld % source-maps?) (document/declares m))
        v/document:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld :source-map [m source-maps?]
  (debug "Generating SourceMap" (document/id m))
  (->> {"@id" (document/id m)
        "@type" [v/document:SourceMap]
        v/document:location [{"@id" (document/source m)}]
        v/document:tag      (map #(to-jsonld % source-maps?) (document/tags m))}
       (utils/clean-nils)))

(defmethod to-jsonld :tag [m source-maps?]
  (debug "Generating Tag" (document/id m))
  (->> {"@id" (document/id m)
        "@type" [v/document:Tag]
        v/document:tag-id [{"@value" (document/tag-id m)}]
        v/document:tag-value [{"@value" (document/value m)}]}
       (utils/clean-nils)))

(defmethod to-jsonld :fragment [m source-maps?]
  (->> {"@id" (document/id m)
        "@type" [v/document:Fragment
                 v/document:Unit]
        v/document:encodes [(to-jsonld (document/encodes m) source-maps?)]}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld nil [_ _] nil)
