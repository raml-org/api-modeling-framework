(ns api-modelling-framework.generators.document.openapi
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.openapi :as domain-generator]
            [api-modelling-framework.generators.domain.common :as common]
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
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        uses (common/process-anonymous-libraries-list uses model)
        library-declares (->> uses
                              (mapv (fn [fragment]
                                      (mapv (fn [fragment] (assoc fragment :from-library true))
                                            (document/declares fragment))))
                              (apply concat))
        annotations (common/model->annotationTypes declares ctx domain-generator/to-openapi!)
        uses (mapv (fn [library] (to-openapi library ctx)) uses)
        context (-> ctx
                    (assoc :document-location (document/location model))
                    (assoc :references (concat declares library-declares))
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-openapi)
                    (assoc :annotations annotations))
        encoded (domain-generator/to-openapi (document/encodes model) context)
        encoded (if (> (count uses) 0) (assoc encoded :x-uses uses) encoded)]
    {syntax/at-location (document/location model)
     syntax/at-data encoded
     syntax/at-fragment "OpenAPI"}))

(defn clean-auto-generated [node]
  node
  (if (some? (:x-generated node))
    nil
    (cond
      (map? node) (let [cleaned (->> node
                                     (map (fn [[k v]] [k (clean-auto-generated v)]))
                                     (into {})
                                     utils/clean-nils)]
                    (if (= cleaned {})
                      nil
                      cleaned))
      (coll? node) (let [cleaned (->> node
                                      (map clean-auto-generated)
                                      (filter some?))]
                     (if (empty? cleaned) nil cleaned))
      :else node)))

(clean-auto-generated nil)

(defmethod to-openapi :fragment [model ctx]
  (debug "Generating Fragment at " (document/location model))
  (let [fragments (if (:fragments ctx)
                    (:fragments ctx)
                    (->> (document/references model)
                         (reduce (fn [acc fragment]
                                   (assoc acc (document/location fragment) fragment))
                                 {})))
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        uses (common/process-anonymous-libraries-list uses model)
        library-declares (->> uses
                              (mapv (fn [fragment]
                                      (mapv (fn [fragment] (assoc fragment :from-library true))
                                            (document/declares fragment))))
                              (apply concat))
        uses (mapv (fn [library] (to-openapi library ctx)) uses)

        context (-> ctx
                    (assoc :fragments fragments)
                    (assoc :references library-declares)
                    (assoc :document-location (document/location model))
                    (assoc :expanded-fragments (or (:expanded-fragments ctx)
                                                   (atom {})))
                    (assoc :document-generator to-openapi))
        fragment (domain-generator/to-openapi (document/encodes model) context)
        fragment (if (> (count uses) 0) (assoc fragment :x-uses uses) fragment)
        fragment (if (and (some? (document/encodes model))
                          (satisfies? domain/DomainElement (document/encodes model))
                          (domain/abstract (document/encodes model)))
                   (clean-auto-generated (assoc fragment :x-abstract-node true))
                   fragment)]
    {syntax/at-location (document/location model)
     syntax/at-data fragment}))

(defmethod to-openapi :unknown [model ctx]
  (debug "Unknown document fragment trying domain model generator")
  (domain-generator/to-openapi model ctx))

(defmethod to-openapi :library [model ctx]
  (debug "Generating Document at " (document/location model))
  (let [declares (document/declares model)
        fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        annotations (common/model->annotationTypes declares ctx domain-generator/to-openapi!)
        context (-> ctx
                    (assoc :document-location (document/location model))
                    (assoc :references declares)
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-openapi))
        types (common/model->types (assoc context :resolve-types true) domain-generator/to-openapi!)
        traits (common/model->traits context domain-generator/to-openapi!)
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        uses (mapv (fn [library] (to-openapi library ctx)) uses)]
    {syntax/at-location (document/location model)
     syntax/at-data (-> {:swagger "Swagger Library"
                         :types types
                         :traits traits
                         :x-annotationTypes annotations
                         :x-uses uses}
                        (utils/clean-nils))
     syntax/at-fragment "OpenAPI Library"}))
