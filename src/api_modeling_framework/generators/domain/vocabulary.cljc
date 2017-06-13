(ns api-modeling-framework.generators.domain.vocabulary
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.generators.domain.common :as common]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn id-term [term {:keys [vocabularies]}]
  (let [term (utils/safe-str term)
        alias-prefix (->> vocabularies
                          (filter (fn [[prefix vocab]] (= 0 (string/index-of term vocab))))
                          first)]
    (if (some? alias-prefix)
      (let [[alias prefix] alias-prefix]
        (if (= alias "")
          (string/replace term prefix "")
          (str (utils/safe-str alias) "." (string/replace term prefix ""))))
      term)))

(defn generate-property-range [range {:keys [base] :as context}]
  (condp = range
    (v/xsd-ns "string") "string"
    (v/xsd-ns "integer") "integer"
    (v/xsd-ns "float") "float"
    (v/xsd-ns "boolean") "boolean"
    (v/xsd-ns "anyURI") "anyURI"
    (cond
      (string? range) (id-term range context)
      :else           nil)))

(defn generate-property [property-model ctx]
  (let [term (id-term (document/id property-model) ctx)
        name (document/name property-model)
        description (document/description property-model)
        range(generate-property-range (domain/range property-model) ctx)
        extends (mapv #(id-term % ctx) (document/extends property-model))]
    [term (utils/clean-nils
           {:displayName name
            :description description
            :extends (if (= 1 (count extends)) (first extends) extends)
            :range range})]))

(defn class-domains-map [properties]
  (->> properties
       (mapv (fn [property] (->> (domain/domain property)
                                (mapv (fn [class-id]
                                        {:id class-id :prop property})))))
       flatten
       (reduce (fn [acc {:keys [id prop]}]
                 (let [ranges (get acc id [])]
                   (assoc acc id (conj ranges (document/id prop)))))
               {})))


(defn generate-class [class-model {:keys [class-domains-map] :as ctx}]
  (let [term (id-term (document/id class-model) ctx)
        name (document/name class-model)
        description (document/description class-model)
        extends (mapv #(id-term % ctx) (document/extends class-model))
        properties (->> (get class-domains-map (document/id class-model))
                        (mapv (fn [prop-id] (id-term prop-id ctx))))]
    [term (utils/clean-nils
           {:displayName name
            :description description
            :extends (if (= 1 (count extends)) (first extends) extends)
            :properties properties})]))


(defn generate-properties [model ctx]
  (->>(domain/properties model)
      (mapv #(generate-property % ctx))
      (filter (fn [[x m]] (not= m {})))))

(defn generate-classes [model ctx]
  (let [classes (domain/classes model)]
    (mapv #(generate-class % ctx) classes)))

(defn generate [model {:keys [vocabularies] :as ctx}]
  (let [base (domain/base model)
        class-domains-map (class-domains-map (domain/properties model))
        ctx (assoc ctx :vocabularies (assoc vocabularies "" base))
        ctx (assoc ctx :class-domains-map class-domains-map)
        properties (generate-properties model ctx)
        classes (generate-classes model ctx)]
    {:base (domain/base model)
     :usage (document/description model)
     :propertyTerms (into {} properties)
     :classTerms (into {} classes)}))
