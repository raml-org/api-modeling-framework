(ns api-modelling-framework.parser.domain.raml-types-shapes
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(declare parse-type)

(defn inline-json-schema? [node]
  (and (string? node) (string/starts-with? node "{")))

(defn parse-generic-keywords [node shape]
  (->> node
       (map (fn [[p v]]
              (condp = p
                :displayName #(assoc % (v/hydra-ns "title") [{"@value" v}])
                :description #(assoc % (v/hydra-ns "description") [{"@value" v}])
                identity)))
       (reduce (fn [acc p] (p acc)) shape)))

(defn parse-type-constraints [node shape]
  (if (map? node)
    (->> node
         (map (fn [[p v]]
                (condp = p
                  :minLength  #(assoc % (v/sh-ns "minLength") [{"@value" v}])
                  :maxLength  #(assoc % (v/sh-ns "maxLength") [{"@value" v}])
                  :pattern    #(assoc % (v/sh-ns "pattern")   [{"@value" v}])
                  :uniqueItems #(assoc % (v/shapes-ns "uniqueItems") [{"@value" v}])
                  identity)))
         (reduce (fn [acc p] (p acc)) shape)
         (parse-generic-keywords node))
    shape))

(defn parse-shape [node {:keys [parsed-location] :as context}]
  (let [parsed-location (str parsed-location "/shape")
        properties (->> (:properties node [])
                        (map (fn [[k v]]
                               (let [parsed-location (str parsed-location "/shape")
                                     property-name (utils/safe-str k)
                                     required (if (some? (:required v))
                                                (:required v)
                                                (if (string/ends-with? property-name "?")
                                                  true
                                                  nil))
                                     property-name (if (some? (:required v))
                                                     property-name
                                                     (string/replace property-name #"\?$" ""))]
                                 (->> {"@type" [(v/sh-ns "PropertyConstraint")]
                                       "@id" parsed-location
                                       (v/shapes-ns "propertyLabel") [{"@value" property-name}]
                                       ;; mandatory prop?
                                       (v/sh-ns "minCount") (if (some? required)
                                                              (if required [{"@value" 1}] [{"@value" 0}])
                                                              nil)
                                       ;; range of the prop
                                       (v/shapes-ns "range") [(parse-type v (assoc context :parsed-location parsed-location))]}
                                      utils/clean-nils
                                      (parse-type-constraints v))))))
        open-shape (:additionalProperties node)]
        (->> {"@type" [(v/sh-ns "Shape")]
             "@id" parsed-location
              (v/sh-ns "property") properties
              (v/sh-ns "closed") (if (some? open-shape)
                                   [{"@value" (not open-shape)}]
                                   nil)}
            utils/clean-nils
            (parse-type-constraints node)
            )))

(defn parse-array [node {:keys [parsed-location] :as context}]
  (let [parsed-location (str parsed-location "/array-shape")
        required-set (set (:required node []))
        is-tuple (some? (get node (keyword "(is-tuple)")))
        item-types (if is-tuple
                     (-> node :items :of)
                     [(:items node {:type "any"})])
        items  (map (fn [i item-type]
                      (parse-type item-type (assoc context :parsed-location (str parsed-location "/items/" i))))
                    (range 0 (count item-types))
                    item-types)
        label (let [items-label (->> items (map #(get % v/sorg:name)) flatten (filter some?) first)]
                (if (some? items-label)
                  (str (get items-label "@value") "[]")
                  nil))]

    (->> {"@type" [(v/sh-ns "Shape")
                   (v/shapes-ns "Array")]
          "@id" parsed-location
          v/sorg:name (if (some? label) [{"@value" label}] nil)
          (v/shapes-ns "item") items}
         (utils/clean-nils)
         (parse-type-constraints node))))

(defn parse-scalar [parsed-location scalar-type]
  {"@id" (str parsed-location "/scalar-shape")
   v/sorg:name [{"@value" "XML-Schema Type"}]
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "Scalar")]
   (v/sh-ns "dataType") [{"@id" scalar-type}]})

(defn parse-json-node [parsed-location text]
  {"@id" (str parsed-location "/json-schema-shape")
   v/sorg:name [{"@value" "JSON-Schema Type"}]
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "JSONSchema")]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})

(defn parse-xml-node [parsed-location text]
  {"@id" (str parsed-location "/xml-schema-shape")
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "XMLSchema")]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})

(defn check-reference
  "Checks if a provided string points to one of the types defined at the APIDocumentation level"
  [type-string {:keys [references parsed-location]}]

  (if-let [type-reference (get references (keyword type-string))]
    (let [remote-id (-> type-reference domain/shape (get "@id"))
          label (or (-> type-reference :name)
                    (-> type-reference domain/shape :name)
                    (str "#" (last (string/split remote-id #"#"))))]
      {"@id" (str parsed-location "/ref-shape")
       v/sorg:name [{"@value" (str "#" label)}]
       "@type" [(v/shapes-ns "Shape")]
       (v/shapes-ns "inherits") [remote-id]})
    nil))

(defn check-multiple-inheritance
  "Computes multiple-inheritance references"
  [types {:keys [parsed-location default-type] :as context}]
  (let [types (mapv #(parse-type % context) types)]
    {"@id" (str parsed-location "/ref-shape")
     "@type" [(v/shapes-ns "Shape")]
     (v/shapes-ns "inherits") types}))

(defn check-inheritance
  [node {:keys [location parsed-location] :as context}]
  (let [parsed-location (utils/path-join parsed-location "type")
        location (utils/path-join location "type")]
    {"@id"  parsed-location
     "@type" [(v/shapes-ns "Shape")]
     (v/shapes-ns "inherits") [(parse-type (:type node) (-> context
                                                            (assoc :parsed-location parsed-location)
                                                            (assoc :location location)))]}))

(defn check-inclusion [node {:keys [parse-ast parsed-location] :as context}]
  (let [parsed (parse-ast node context)
        location (syntax/<-location node)]
    {"@id" (str parsed-location "/include-shape")
     "@type" [(v/shapes-ns "Shape")]
     (v/shapes-ns "inherits") [location]}))

(defn parse-type [node {:keys [parsed-location default-type] :as context}]
  (cond
    (some? (syntax/<-data node)) (check-inclusion node context)

    (string? node)               (cond
                                   (string/starts-with? node "{") (parse-json-node parsed-location node)
                                   (string/starts-with? node "<") (parse-xml-node parsed-location node)
                                   :else (parse-type {:type node} context))

    (map? node)                  (let [type-ref (or (:type node) (:schema node) (or default-type "object"))]
                                   (condp = type-ref
                                     "string"  (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "string")))
                                     "number"  (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "float")))
                                     "integer"  (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "integer")))
                                     "float"  (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "float")))
                                     "boolean" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "boolean")))
                                     "null" (parse-type-constraints node (parse-scalar parsed-location (v/shapes-ns "null")))
                                     "time-only" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "time")))
                                     "datetime" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "dateTime")))
                                     "datetime-only" (parse-type-constraints node (parse-scalar parsed-location (v/shapes-ns "datetime-only")))
                                     "date-only" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "date")))
                                     "any" (parse-type-constraints node (parse-scalar parsed-location (v/shapes-ns "any")))
                                     "object"  (parse-shape node context)
                                     "array"   (parse-array node context)
                                     (let [shape (cond
                                                   (map? type-ref)               (check-inheritance node context)
                                                   (coll? type-ref)              (check-multiple-inheritance node context)
                                                   (string? type-ref)            (cond
                                                                                   (string/starts-with? type-ref "{") (parse-json-node parsed-location type-ref)
                                                                                   (string/starts-with? type-ref "<") (parse-xml-node parsed-location type-ref)
                                                                                   :else (check-reference type-ref context))
                                                   :else                           nil)]
                                       (if (some? shape)
                                         (parse-type-constraints node shape)
                                         nil))))
    :else nil))
