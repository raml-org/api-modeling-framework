(ns api-modelling-framework.generators.document.raml
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.raml :as domain-generator]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-raml-dispatch-fn [model ctx]
  (cond
    (nil? model)                                  model

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))      :document

    (satisfies? document/Fragment model)          :fragment

    (satisfies? document/Module model)            :library))


(defmulti to-raml (fn [model ctx] (to-raml-dispatch-fn model ctx)))


(defmethod to-raml :document [model ctx]
  (debug "Generating Document at " (document/location model))
  (let [declares (document/declares model)
        fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        context (-> ctx
                    (assoc :references declares)
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-raml))]
    {(keyword "@location") (document/location model)
     (keyword "@data") (domain-generator/to-raml (document/encodes model) context)
     (keyword "@fragment") "#%RAML 1.0"}))


(defmethod to-raml :fragment [model ctx]
  (debug "Generating Fragment at " (document/location model))
  (let [fragments (if (:fragments ctx)
                    (:fragments ctx)
                    (->> (document/references model)
                         (reduce (fn [acc fragment]
                                   (assoc acc (document/location fragment) fragment))
                                 {})))
        context (-> ctx
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (or (:expanded-fragments ctx)
                                                   (atom {})))
                    (assoc :type-hint :method)
                    (assoc :document-generator to-raml))
        fragment-type-tag (first (document/find-tag model document/document-type-tag))
        fragment-type (if (some? fragment-type-tag) (document/value fragment-type-tag) nil)]
    (utils/clean-nils {(keyword "@location") (document/location model)
                       (keyword "@data") (utils/clean-nils
                                          (merge (domain-generator/to-raml (domain/to-domain-node (document/encodes model)) context)
                                                 {:usage (document/description model)}))
                       (keyword "@fragment") fragment-type})))
