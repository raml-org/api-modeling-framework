(ns api-modelling-framework.generators.document.jsonld
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.jsonld :as domain-generator]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn ensure-domain-fragment [x]
  (try
    (domain/to-domain-node x)
    (catch #?(:clj Exception :cljs js/Error) e
      x)))

(defn to-jsonld-dispatch-fn [model source-maps?]
  (cond
    (nil? model)                                 model

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))     :document

    (satisfies? document/Fragment model)         :fragment

    (satisfies? document/Module model)           :library

    :else                                        :unknown))

(defmulti to-jsonld (fn [model source-maps?] (to-jsonld-dispatch-fn model source-maps?)))

(defn with-source-maps
  "Adds source maps to a model if required"
  [source-maps? model generated]
  (if source-maps?
    (let [source-maps (document/sources model)]
      (assoc generated v/document:source (map #(domain-generator/to-jsonld % {:source-maps? source-maps?}) source-maps)))
    generated))

(defmethod to-jsonld :document [m source-maps?]
  (debug "Generating Document")
  (->> {"@id" (document/id m)
        "@type" [v/document:Document
                 v/document:Fragment
                 v/document:Module
                 v/document:Unit]
        v/document:declares (mapv #(to-jsonld (ensure-domain-fragment %) source-maps?) (document/declares m))
        v/document:encodes [(domain-generator/to-jsonld (document/encodes m) {:source-maps? source-maps?})]
        v/document:references (mapv #(to-jsonld (ensure-domain-fragment %) source-maps?) (document/references m))}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld :fragment [m source-maps?]
  (debug "Generating Fragment")
  (->> {"@id" (document/id m)
        "@type" [v/document:Fragment
                 v/document:Unit]
        v/document:encodes [(domain-generator/to-jsonld (document/encodes m) {:source-maps? source-maps?})]
        v/document:references (mapv #(to-jsonld (ensure-domain-fragment %) source-maps?) (document/references m))}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld :library [m source-maps?]
  (debug "Generating Library")
  (->> {"@id" (document/id m)
        "@type" [v/document:Module
                 v/document:Unit]
        v/document:declares (mapv #(to-jsonld (ensure-domain-fragment %) source-maps?) (document/declares m))
        v/document:references (mapv #(to-jsonld (ensure-domain-fragment %) source-maps?) (document/references m))}
       (with-source-maps source-maps? m)
       (utils/clean-nils)))

(defmethod to-jsonld nil [_ _] nil)

(defmethod to-jsonld :unknown [model source-maps?]
  (domain-generator/to-jsonld model {:source-maps? source-maps?}))
