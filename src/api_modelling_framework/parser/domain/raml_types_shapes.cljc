(ns api-modelling-framework.parser.domain.raml-types-shapes
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.model.document :as document]
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
                :displayName #(assoc % v/sorg:name [{"@value" v}])
                :description #(assoc % v/sorg:description [{"@value" v}])
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
                  :format     #(assoc % (v/shapes-ns "format") [{"@value" v}])
                  :additionalProperties #(assoc % (v/sh-ns "closed") [{"@value" (not (utils/->bool v))}])
                  :uniqueItems #(assoc % (v/shapes-ns "uniqueItems") [{"@value" v}])
                  :multipleOf #(assoc % (v/shapes-ns "multipleOf") [{"@value" v}])
                  :minimum    #(assoc % (v/sh-ns "minExclusive") [{"@value" v}])
                  identity)))
         (reduce (fn [acc p] (p acc)) shape)
         (parse-generic-keywords node))
    shape))

(defn required-property? [property-name v]
  (if (some? (:required v))
    (:required v)
    (if (string/ends-with? property-name "?")
      false
      true)))

(defn final-property-name [property-name v]
  (if (some? (:required v))
    (utils/safe-str property-name)
    (string/replace (utils/safe-str property-name) #"\?$" "")))

(defn scalar-shape->property-shape [shape]
  {;; Object properties vs arrays, only one is allowed if it is an object (or scalar)
   (v/sh-ns "maxCount")  [{"@value" 1}]
   ;; instead of node, we have a datatype here
   (v/sh-ns "dataType")  (get shape (v/sh-ns "dataType"))})

(defn array-shape->property-shape [shape]
  (let [items (get shape (v/shapes-ns "item"))
        range (if (= 1 (count items))
                (first items)
                {(v/sh-ns "or") {"@list" items}})]
    {;; we mark it for our own purposes, for example being able to detect
     ;; it easily without checking cardinality
     (v/shapes-ns "ordered") [{"@value" true}]
     ;; range of the prop
     (v/sh-ns "node")        [range]}))

(defn node-shape->property-shape [shape]
  {;; Object properties vs arrays, only one is allowed if it is an object
   (v/sh-ns "maxCount")  [{"@value" 1}]
   ;; range of the prop
   (v/sh-ns "node")     [shape]})

(defn parse-shape [node {:keys [parsed-location] :as context}]
  (let [properties (->> (:properties node [])
                        (map (fn [[k v]]
                               (let [property-name (utils/safe-str k)
                                     property-name (final-property-name property-name v)
                                     parsed-location (utils/path-join parsed-location (str "/property/" property-name))
                                     parsed-property-target (parse-type v (assoc context :parsed-location parsed-location))
                                     property-shape (cond
                                                      (utils/scalar-shape? parsed-property-target) (scalar-shape->property-shape parsed-property-target)
                                                      (utils/array-shape? parsed-property-target)  (array-shape->property-shape parsed-property-target)
                                                      :else (node-shape->property-shape parsed-property-target))
                                     required (required-property? property-name v)
                                     ;; common properties
                                     property-shape (-> property-shape
                                                        (assoc "@id" parsed-location)
                                                        (assoc "@type" [(v/sh-ns "PropertyShape") (v/sh-ns "Shape")])
                                                        (assoc (v/sh-ns "path") [{"@id" (v/anon-shapes-ns property-name)}])
                                                        (assoc (v/shapes-ns "propertyLabel") [{"@value" property-name}])
                                                        ;; mandatory prop?
                                                        (assoc (v/sh-ns "minCount") [(if required {"@value" 1} {"@value" 0})])
                                                        utils/clean-nils)]
                                 (parse-type-constraints v property-shape)))))
        open-shape (:additionalProperties node)]
    (->> {"@type" [(v/sh-ns "NodeShape") (v/sh-ns "Shape")]
          "@id" parsed-location
          (v/sh-ns "property") properties
          (v/sh-ns "closed") (if (some? open-shape)
                               [{"@value" (not open-shape)}]
                               nil)}
         utils/clean-nils
         (parse-type-constraints node)
         )))

(defn parse-array [node {:keys [parsed-location] :as context}]
  (let [required-set (set (:required node []))
        is-tuple (some? (get node (keyword "(is-tuple)")))
        item-types (if is-tuple
                     (-> node :items :of)
                     [(:items node {:type "any"})])
        items  (map (fn [i item-type]
                      (parse-type item-type (assoc context :parsed-location (str parsed-location "/items/" i))))
                    (range 0 (count item-types))
                    item-types)]

    (->> {"@type" [(v/shapes-ns "Array")
                   (v/sh-ns "Shape")]
          "@id" parsed-location
          (v/shapes-ns "item") items}
         (utils/clean-nils)
         (parse-type-constraints node))))

(defn parse-scalar [parsed-location scalar-type]
  (-> {"@id" parsed-location
       "@type" [(v/shapes-ns "Scalar") (v/sh-ns "Shape")]
       (v/sh-ns "dataType") (if (= "shapes:any" scalar-type)
                              nil
                              [{"@id" scalar-type}])}
      utils/clean-nils))

(defn parse-json-node [parsed-location text]
  {"@id" parsed-location
   "@type" [(v/shapes-ns "JSONSchema") (v/sh-ns "Shape")]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})

(defn parse-xml-node [parsed-location text]
  {"@id" parsed-location
   "@type" [(v/shapes-ns "XMLSchema") (v/sh-ns "Shape") ]
   (v/shapes-ns "schemaRaw") [{"@value" text}]})


(defn check-multiple-inheritance
  "Computes multiple-inheritance references"
  [types {:keys [parsed-location default-type] :as context}]
  (let [types (mapv #(parse-type % context) types)]
    {"@id" parsed-location
     "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
     (v/shapes-ns "inherits") types}))

(defn check-inheritance
  [node {:keys [location parsed-location] :as context}]
  (let [location (utils/path-join location "type")
        child (cond
                (some? (:properties node)) (parse-type (assoc node :type "object") context)
                (some? (:items node))      (parse-type (assoc node :type "array") context)
                :else {"@id"  parsed-location
                       "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]})]
    (assoc child (v/shapes-ns "inherits") [(parse-type (:type node) (-> context
                                                                        (assoc :parsed-location (utils/path-join parsed-location "type"))
                                                                        (assoc :location location)))])))

(defn check-inclusion [node {:keys [parse-ast parsed-location] :as context}]
  (let [parsed (parse-ast node context)
        location (syntax/<-location node)]
    {"@id" parsed-location
     "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
     (v/shapes-ns "inherits") [{"@id" location}]}))

(defn check-reference
  "Checks if a provided string points to one of the types defined at the APIDocumentation level"
  [type-string {:keys [references parsed-location base-uri] :as context}]

  (if-let [type-reference (get references (keyword type-string))]
    (let [label (or (-> type-reference :name)
                    (if (satisfies? domain/Type type-reference) (-> type-reference domain/shape :name) nil)
                    type-string)]
      (cond
        (some? (:x-ahead-declaration type-reference)) {"@id" parsed-location
                                                       "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
                                                       v/sorg:name [{"@value" label}]
                                                       (v/shapes-ns "inherits") [{"@id" (:x-ahead-declaration type-reference)}]}

        (satisfies? document/Includes type-reference) {"@id" parsed-location
                                                       "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
                                                       (v/shapes-ns "inherits") [{"@id" (document/target type-reference)}]}
        :else
        (let [remote-id (-> type-reference domain/shape (get "@id"))
              label (or (-> type-reference :name)
                        (-> type-reference domain/shape :name)
                        (last (string/split remote-id #"#")))]
          {"@id" parsed-location
           v/sorg:name [{"@value" label}]
           "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
           (v/shapes-ns "inherits") [{"@id" remote-id}]})))

    ;; we always try to return a reference
    {"@id" parsed-location
     "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
     v/sorg:name [{"@value" (utils/hash-path type-string)}]
     (v/shapes-ns "inherits") [{"@id" (if (= 0 (string/index-of type-string "#"))
                                        (str base-uri type-string)
                                        type-string)}]}))

(defn ensure-type-property [node]
  (let [type (:type node (:schema node))]
    (-> node
        (dissoc :type)
        (dissoc :schema)
        (assoc :type type))))

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
                                                                                   :else (if (or (= [:type] (keys node))
                                                                                                 (= [:schema] (keys node)))
                                                                                           (check-reference type-ref context)
                                                                                           (check-inheritance (ensure-type-property node) context)))
                                                   :else                           nil)]
                                       (if (some? shape)
                                         (parse-type-constraints node shape)
                                         nil))))
    :else nil))
