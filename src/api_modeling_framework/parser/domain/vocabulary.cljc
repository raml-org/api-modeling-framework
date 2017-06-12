(ns api-modeling-framework.parser.domain.vocabulary
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.parser.domain.common :as common]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.model.domain :as domain]
            [clojure.string :as string]))

(defn ensure-list [x] (flatten [x]))

(defn term-id [prefix name {:keys [vocabularies]}]
  (let [safe-name (utils/safe-str name)]
    (if (string/index-of safe-name ".")
      (let [[prefix suffix] (string/split safe-name #"\.")
            base (get vocabularies prefix)]
        (str base suffix))
      (str prefix safe-name))))

(defn parse-base [node {:keys [location]}] (common/ast-get node :base location))

(defn parse-usage [node _] (common/ast-get node :usage))

(defn parse-extends [node {:keys [base] :as context}]
  (-> node
      (common/ast-get :extends [])
      (ensure-list)
      (->> (mapv (fn [id] (term-id base id context))))))

(defn parse-class [[name node] {:keys [base] :as context}]
  (domain/map->ParsedClassTerm {:id (term-id base name context)
                                :name (common/ast-get node :displayName)
                                :extends (parse-extends node context)
                                :description (common/ast-get node :description)
                                :properties (->> (common/ast-get node :properties [])
                                                 (ensure-list)
                                                 (mapv #(term-id base % context)))}))

(defn parse-classes [node context]
  (->> (common/ast-get node :classTerms [])
       (mapv #(parse-class % context))))

(defn property-domains-map [classes]
  (->> classes
       (mapv (fn [{:keys [id properties]}]
               (mapv (fn [prop] {:id id :prop prop}) properties)))
       flatten
       (reduce (fn [acc {:keys [id prop]}]
                 (let [ranges (get acc prop [])]
                   (assoc acc prop (conj ranges id))))
               {})))

(defn parse-property-range [range {:keys [base] :as context}]
  (condp = range
    "string"  (v/xsd-ns "string")
    "integer" (v/xsd-ns "integer")
    "float"   (v/xsd-ns "float")
    "boolean" (v/xsd-ns "boolean")
    (cond
      (string? range) (term-id base range context)
      :else           nil)))

(defn parse-property-type [range]
  (condp = range
    "string"  "datatype"
    "integer" "datatype"
    "float"   "datatype"
    "boolean" "datatype"
    nil       "object"
    "object"))

(defn parse-property [[id node] {:keys [base domains-map] :as context}]
  (let [id (term-id base id context)]
    (domain/map->ParsedPropertyTerm {:id id
                                     :name (common/ast-get node :displayName)
                                     :extends (parse-extends node context)
                                     :description (common/ast-get node :description)
                                     :range (parse-property-range (common/ast-get node :range) context)
                                     :property-type (parse-property-type (common/ast-get node :range))
                                     :domain (get domains-map id nil)})))

(defn parse-properties [node context]
  (->> (common/ast-get node :propertyTerms [])
       (mapv #(parse-property % context))))


(defn parse-domain-referenced-properties [properties {:keys [domains-map]}]
  (let [parsed-properties-map (->> properties
                                   (map (fn [prop] [(:id prop) true]))
                                   (into {}))]
    (->> domains-map
         (filter (fn [[property domain-classes]]
                   (nil? (get parsed-properties-map property))))
         (mapv (fn [[property domain-classes]]
                 (domain/map->ParsedPropertyTerm {:id property
                                                  :domain domain-classes}))))))

(defn parse [node context]
  (let [base (parse-base node context)
        context (assoc context :base base)
        usage (parse-usage node context)
        classes (parse-classes node context)
        context (assoc context :domains-map (property-domains-map classes))
        properties (parse-properties node context)
        domain-referenced-properties (parse-domain-referenced-properties properties context)]
    (domain/map->ParsedVocabulary {:base base
                                   :description usage
                                   :classes (mapv #(dissoc % :properites) classes)
                                   :properties (concat properties
                                                           domain-referenced-properties)})))
