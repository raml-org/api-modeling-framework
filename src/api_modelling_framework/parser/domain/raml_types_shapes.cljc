(ns api-modelling-framework.parser.domain.raml-types-shapes
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(declare parse-type)

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
                    item-types)]

    (->> {"@type" [(v/sh-ns "Shape")
                   (v/shapes-ns "Array")]
          "@id" parsed-location
          (v/shapes-ns "item") items}
         (parse-type-constraints node))))

(defn parse-scalar [parsed-location scalar-type]
  {"@id" (str parsed-location "/scalar-shape")
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "Scalar")]
   (v/sh-ns "dataType") [{"@id" scalar-type}]})

(defn parse-json-node [parsed-location text]
  {"@id" (str parsed-location "/json-schema-shape")
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "JSONSchema")]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})

(defn parse-xml-node [parsed-location text]
  {"@id" (str parsed-location "/xml-schema-shape")
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "XMLSchema")]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})

(defn parse-type [node {:keys [parsed-location] :as context}]
  (let [type-string (if (string? node)
                      (cond
                        (string/starts-with? node "{") (parse-json-node parsed-location node)
                        (string/starts-with? node "<") (parse-xml-node parsed-location node)
                        :else nil)
                      (or (:type node) (:schema node)))
        shape (condp = type-string
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
                nil)]
    shape))
