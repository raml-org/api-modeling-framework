(ns api-modelling-framework.generators.document.openapi
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.openapi :as domain-generator]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model) nil

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))      :document

    (satisfies? document/Fragment model)          :fragment

    (satisfies? document/Module model)            :library

    :else                                         :unknown))


(defmulti to-openapi (fn [model ctx] (to-openapi-dispatch-fn model ctx)))


(defmethod to-openapi :document [model ctx]
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
                    (assoc :document-generator to-openapi))]
    {syntax/at-location (document/location model)
     syntax/at-data (domain-generator/to-openapi (document/encodes model) context)
     syntax/at-fragment "OpenAPI"}))


(defmethod to-openapi :fragment [model ctx]
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
                    (assoc :document-generator to-openapi))]
    {syntax/at-location (document/location model)
     syntax/at-data (domain-generator/to-openapi (domain/to-domain-node (document/encodes model)) context)}))

(defmethod to-openapi :unknown [model ctx]
  (debug "Unknown document fragment trying domain model generator")
  (domain-generator/to-openapi model ctx))
