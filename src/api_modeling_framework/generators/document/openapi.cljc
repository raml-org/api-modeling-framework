(ns api-modeling-framework.generators.document.openapi
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.generators.domain.openapi :as domain-generator]
            [api-modeling-framework.generators.domain.common :as common]
            [api-modeling-framework.utils :as utils]))

(defn to-openapi-dispatch-fn [model ctx]
  (cond
    (nil? model) nil

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))      :document

    (satisfies? document/Fragment model)          :fragment

    (satisfies? document/Module model)            :library

    :else                                         :unknown))


(defmulti to-openapi (fn [model ctx] (to-openapi-dispatch-fn model ctx)))

(defn model->annotationType-ref [declares]
  (->> declares
       (filter (fn [declare] (some? (-> declare (document/find-tag document/is-annotation-tag) first))))
       (mapv (fn [annotation]
               [(-> annotation (document/find-tag document/is-annotation-tag) first document/value)
                (document/id annotation)]))
       (into {})))


(defmethod to-openapi :document [model ctx]
  (utils/debug "Generating Document at " (document/location model))
  (let [ctx (assoc ctx :syntax :openapi)
        fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        uses (common/process-anonymous-libraries-list uses model)
        declares (document/declares model)
        library-declares (->> uses
                              (mapv (fn [fragment]
                                      (mapv (fn [fragment] (assoc fragment :from-library true))
                                            (document/declares fragment))))
                              (apply concat))
        ;; transforming uses OpenAPI
        uses (mapv (fn [library] (to-openapi library ctx)) uses)
        annotations (common/model->annotationTypes declares ctx domain-generator/to-openapi!)
        library-annotations (model->annotationType-ref library-declares)
        context (-> ctx
                    (assoc :document-location (document/location model))
                    (assoc :references (concat declares library-declares))
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-openapi)
                    (assoc :annotations (merge annotations library-annotations)))
        encoded (domain-generator/to-openapi (document/encodes model) context)
        encoded (if (> (count uses) 0) (assoc encoded :x-uses uses) encoded)]
    {syntax/at-location (document/location model)
     syntax/at-data encoded
     syntax/at-fragment "OpenAPI"}))

(defmethod to-openapi :fragment [model ctx]
  (utils/debug "Generating Fragment at " (document/location model))
  (let [ctx (assoc ctx :syntax :openapi)
        fragments (if (:fragments ctx)
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
        ;; transforming uses OpenAPI
        uses (mapv (fn [library] (to-openapi library ctx)) uses)
        library-annotations (model->annotationType-ref library-declares)

        context (-> ctx
                    (assoc :fragments fragments)
                    (assoc :references library-declares)
                    (assoc :document-location (document/location model))
                    (assoc :expanded-fragments (or (:expanded-fragments ctx)
                                                   (atom {})))
                    (assoc :document-generator to-openapi)
                    (assoc :annotations library-annotations))
        fragment (domain-generator/to-openapi (document/encodes model) context)
        fragment (if (> (count uses) 0) (assoc fragment :x-uses uses) fragment)
        fragment (domain-generator/check-abstract fragment (document/encodes model))]
    {syntax/at-location (document/location model)
     syntax/at-data fragment}))

(defmethod to-openapi :unknown [model ctx]
  (utils/debug "Unknown document fragment trying domain model generator")
  (domain-generator/to-openapi model ctx))

(defmethod to-openapi :library [model ctx]
  (utils/debug "Generating Document at " (document/location model))
  (let [ctx (assoc ctx :syntax :openapi)
        declares (document/declares model)
        fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        annotations (common/model->annotationTypes declares ctx domain-generator/to-openapi!)
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        library-declares (->> uses
                              (mapv (fn [fragment]
                                      (mapv (fn [fragment] (assoc fragment :from-library true))
                                            (document/declares fragment))))
                              (apply concat))
        library-annotations (model->annotationType-ref library-declares)
        context (-> ctx
                    (assoc :document-location (document/location model))
                    (assoc :references (concat declares library-declares))
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-openapi))
        types (common/model->types (assoc context :resolve-types true) domain-generator/to-openapi!)
        traits (common/model->traits context domain-generator/to-openapi!)
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          (get fragments location))))
        uses (common/process-anonymous-libraries-list uses model)
        library-declares (->> uses
                              (mapv (fn [fragment]
                                      (mapv (fn [fragment] (assoc fragment :from-library true))
                                            (document/declares fragment))))
                              (apply concat))
        uses (mapv (fn [library] (to-openapi library ctx)) uses)]
    {syntax/at-location (document/location model)
     syntax/at-data (-> {:swagger "Swagger Library"
                         :types types
                         :x-traits traits
                         :x-annotationTypes annotations
                         :x-uses uses}
                        (utils/clean-nils))
     syntax/at-fragment "OpenAPI Library"}))
