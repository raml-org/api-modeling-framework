(ns api-modelling-framework.generators.domain.common
  (:require [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]))

(defn find-traits [model context]
  (let [extends (document/extends model)]
    ;; @todo do I need both checks, label and is-trait-tag ??
    (->> extends
         (filter (fn [extend]
                   (= "trait" (name (document/label extend)))))
         (map (fn [trait]
                (let [trait-tag (first (document/find-tag trait document/is-trait-tag))]
                  (if (some? trait-tag)
                    (document/value trait-tag)
                    (-> (document/target trait) (string/split #"/") last))))))))

(defn annotation-reference? [model]
  (-> model
      (document/find-tag document/is-annotation-tag)
      first
      some?))

(defn trait-reference? [model]
  (-> model
      (document/find-tag document/is-trait-tag)
      first
      some?))

(defn model->annotationTypes [declares context domain-generator]
  (->> declares
       (filter (fn [declare] (some? (-> declare (document/find-tag document/is-annotation-tag) first))))
       (mapv (fn [annotation]
               [(-> annotation (document/find-tag document/is-annotation-tag) first document/value) (domain-generator annotation context)]))
       (into {})))

(defn model->traits [{:keys [references] :as ctx} domain-generator]
  (->> references
       (filter (fn [ref] (nil? (:from-library ref))))
       (filter trait-reference?)
       (map (fn [reference]
              (let [is-trait-tag (-> reference
                                     (document/find-tag document/is-trait-tag)
                                     first)
                    trait-name (-> is-trait-tag
                                   (document/value)
                                   keyword)
                    method (if (some? reference)
                             (domain/to-domain-node reference)
                             (throw (new #?(:cljs js/Error :clj Exception) (str "Cannot find extended trait " trait-name))))
                    generated (domain-generator method ctx)]
                [trait-name generated])))
       (into {})))

(defn type-reference-name [reference]
  (let [is-type-tag (-> reference
                        (document/find-tag document/is-type-tag)
                        first)
        type-name (-> is-type-tag
                      (document/value))]
    type-name))

(defn model->types [{:keys [references] :as ctx} domain-generator]
  (->> references
       (filter (fn [ref] (nil? (:from-library ref))))
       (filter (fn [reference]
                 (let [is-type-tag (-> reference
                                       (document/find-tag document/is-type-tag)
                                       first)]
                   (some? is-type-tag))))
       (map (fn [reference]
              (let [type-name (type-reference-name reference)
                    generated (domain-generator reference (assoc ctx :from-library (:from-library reference)))]
                [(keyword type-name) generated])))
       (into {})
       (utils/clean-nils)))

(defn model->uses [node]
  (->> (document/find-tag node document/uses-library-tag)
       (map (fn [tag] [(document/name tag) (document/value tag)]))
       (into {})))


(defn type-reference? [model]
  (-> model
      (document/find-tag document/is-type-tag)
      first
      some?))

(defn ref-shape? [shape-type {:keys [references]}]
  (->> references
       (filter (fn [ref]
                 (satisfies? domain/Type ref)))
       (filter (fn [type]
                 (= (get (domain/shape type) "@id") shape-type)))
       first))
