(ns api-modeling-framework.parser.domain.meta
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.parser.domain.common :as common]
            [api-modeling-framework.parser.domain.raml-types-shapes :as shapes]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.platform :as platform]
            [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.set :as set]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))


(declare parse-ast)

(defn properties-map* [class-term]
  (->> (domain/syntax-rules class-term)
       (mapv (fn [rule] [(domain/syntax-label rule) rule]))
       (into {})))

(def properties-map (memoize properties-map*))

(defn not-annotation? [prop]
  (not= (string/index-of (utils/safe-str prop) "(") 0))

(defn matches-class-properties? [class-term node]
  (let [props (properties-map class-term)]
    (if (> (count props) 0)
      (->> (keys node)
           (filter not-annotation?)
           (filter #(not= (utils/safe-str %) "uses"))
           (mapv (fn [prop] (some? (get props (utils/safe-str prop)))))
           (reduce (fn [acc v] (and acc v)) true))
      false)))

(defn find-vocabulary-class [vocabulary node {:keys [type-hint] :as context}]
  (let [found-class (if (some? type-hint)
                      (->> (or (domain/classes vocabulary) [])
                           (filter (fn [class-term] (= (document/id class-term) type-hint)))
                           first)
                      (->> (or (domain/classes vocabulary) [])
                           (filter #(matches-class-properties? % node))
                           first))]
    (cond
      (some? found-class)              found-class
      (= (v/sh-ns "Shape") type-hint)  (domain/map->ParsedClassTerm {:id (v/sh-ns "Shape")})
      :else                            (throw (new #?(:clj Exception :cljs js/Error)
                                                   (str "Cannot find vocabulary syntax information for " (or type-hint node)))))))


(defn datatype-error [target value]
  (throw (new #?(:clj Exception :cljs js/Error) (str "Scalar type error, expecting " target ", found " value))))

(defn parse-datatype [range  object]
  (condp = range
    "string" (if (string? object) object (datatype-error "string" object))
    (v/xsd-ns "integer") (platform/->int object)
    "integer" (platform/->int object)
    (v/xsd-ns "float") (if (number? object) object (datatype-error "number" object))
    "float" (if (number? object) object (datatype-error "number" object))
    "boolean" (if (or (= true object) (= false object)) object (datatype-error "boolean" object))
    object))

(defn ensure-mandatoriness [property object]
  (if (domain/mandatory property)
    (when (cond (coll? object) (empty? object)
                :else          (nil? object))
      (throw (new #?(:clj Exception :cljs js/Error) (str "Mandatory property " (domain/syntax-label property) " missing in value " object))))))

(defn ensure-collection [property object]
  (when (and (not (domain/collection property))
             (coll? object))
    (throw (new #?(:clj Exception :cljs js/Error) (str "Collection of values not allowed for property  " (domain/syntax-label property) " missing in value " object)))))

(defn parse-property [node syntax-rule vocabulary {:keys [parsed-location] :as context}]
  (debug "Parsing instance of property " (domain/property-id syntax-rule))
  (let [label (domain/syntax-label syntax-rule)
        object (common/ast-get node (keyword label))
        property (common/find-vocabulary-property syntax-rule vocabulary)
        ;;_ (ensure-mandatoriness property object)
        ;;_ (ensure-collection  property object)
        objects (filterv some? (flatten [object]))
        parsed-object (mapv (fn [object i]
                              (let [id (if (= 1 (count objects))
                                         (utils/path-join parsed-location label)
                                         (utils/path-join parsed-location (str label "_" i)))]
                                (condp = (domain/property-type property)
                                  "datatype" (parse-datatype (domain/range property) object)
                                  "object"   (parse-ast vocabulary object (-> context
                                                                              (assoc :parsed-location id)
                                                                              (assoc :type-hint (domain/range property))))
                                  (if (map? object)
                                    (parse-ast vocabulary object (-> context
                                                                     (assoc :parsed-location id)
                                                                     (assoc :type-hint nil)))
                                    (parse-datatype "string" object)))))
                            objects
                            (range 0 (count objects)))]
    (if (= 0 (count parsed-object))
      nil
      (domain/map->ParsedDomainProperty {:id (utils/path-join parsed-location (str "property/" label))
                                         :predicate (document/id property)
                                         :object parsed-object}))))

(defn parse-hash-property [node syntax-rule vocabulary {:keys [parsed-location] :as context}]
  (debug "Parsing instance of hash-property " (domain/property-id syntax-rule))
  (let [property (common/find-vocabulary-property syntax-rule vocabulary)
        hash-property (domain/hash syntax-rule)
        parsed-objects (mapv (fn [[hash-value object]]
                               (parse-ast vocabulary object (-> context
                                                                (assoc :parsed-location (utils/path-join (utils/path-join parsed-location (domain/syntax-label syntax-rule)) hash-value))
                                                                (assoc :type-hint (domain/range property))
                                                                (assoc :hash-property (domain/map->ParsedDomainProperty {:id hash-property :object [(utils/safe-str hash-value)]})))))
                             node)]
    [(domain/map->ParsedDomainProperty {:id (document/id property)
                                        :object parsed-objects})]))

(defn parse-declaration-property [node syntax-rule vocabulary {:keys [parsed-location] :as context}]
  (debug "Parsing instance of declaration-property " (domain/property-id syntax-rule))
  (let [label (domain/syntax-label syntax-rule)
        property (common/find-vocabulary-property syntax-rule vocabulary)
        parsed-objects (mapv (fn [[declaration-value object]]
                               (parse-ast vocabulary object (-> context
                                                                (assoc :parsed-location (utils/path-join (utils/path-join parsed-location (domain/syntax-label syntax-rule)) declaration-value))
                                                                (assoc :type-hint (domain/range property)))))
                             (common/ast-get node (keyword label)))]
    (domain/map->ParsedDomainProperty {:id (document/id property)
                                       :object parsed-objects})))

(defn parse-class-shape [node context]
  (shapes/parse-type node context))

(defn remove-declaration-properties [vocabulary syntax-rules]
  (->> syntax-rules
       (filterv #(not (common/declaration-property? % vocabulary)))))

(defn parse-class [node class-term vocabulary {:keys [parsed-location hash-property references] :as context}]
  (debug "Parsing instance of class " (document/id class-term))
  (if (= (document/id class-term) (v/sh-ns "Shape"))
    (domain/map->ParsedDomainInstance {:id parsed-location
                                       :domain-class (v/sh-ns "Shape")
                                       :shape (parse-class-shape node context)})
    (let [class-id (document/id class-term)
          syntax-rules (domain/syntax-rules class-term)
          syntax-rules (remove-declaration-properties vocabulary syntax-rules)
          non-hash-syntax-rules (->> syntax-rules
                                     (filterv (fn [rule] (and (not (domain/hash rule))
                                                             (not (domain/declaration rule))))))

          declaration-syntax-rule (->> syntax-rules
                                       (filterv (fn [rule] (domain/declaration rule))))

          declaration-properties (->> declaration-syntax-rule
                                      (map #(parse-declaration-property node % vocabulary context)))

          declarations (->> declaration-properties
                            (mapv #(domain/object %))
                            flatten
                            (filter some?)
                            (reduce (fn [acc elem] (assoc acc (document/id elem) elem))
                                    {}))

          context (assoc context references declarations)

          non-hash-properties (->> non-hash-syntax-rules
                                   (mapv #(parse-property node % vocabulary context))
                                   (filter some?))


          remaining-properties (reduce (fn [acc prop]
                                         (dissoc acc (keyword (domain/syntax-label prop))))
                                       node
                                       (filter some? (concat non-hash-syntax-rules declaration-syntax-rule)))
          hash-syntax-rule (->> syntax-rules
                                (filterv (fn [rule] (domain/hash rule)))
                                first)
          hash-properties (if (some? hash-syntax-rule)
                            (parse-hash-property remaining-properties hash-syntax-rule vocabulary context)
                            [])
          inherited-hash-property (if (some? hash-property) [hash-property] [])]
      (domain/map->ParsedDomainInstance {:id parsed-location
                                         :domain-class class-id
                                         :domain-properties (concat hash-properties
                                                                    non-hash-properties
                                                                    inherited-hash-property
                                                                    declaration-properties)}))))

(defn parse-ast [vocabulary node context]
  (let [class-term (find-vocabulary-class vocabulary node context)]
    (parse-class node class-term vocabulary context)))
