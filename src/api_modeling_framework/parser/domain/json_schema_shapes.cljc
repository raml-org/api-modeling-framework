(ns api-modeling-framework.parser.domain.json-schema-shapes
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.platform :as platform]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(declare parse-type)

(defn parse-generic-keywords [node shape]
  (->> node
       (mapv (fn [[p v]]
              (condp = p
                :title       #(assoc % v/sorg:name [{"@value" v}])
                :description #(assoc % v/sorg:description [{"@value" v}])
                identity)))
       (reduce (fn [acc p] (p acc)) shape)))
(defn parse-type-constraints [node shape]
  (->> node
       (mapv (fn [[p v]]
              (condp = p
                :minLength  #(assoc % (v/sh-ns "minLength") [{"@value" (platform/->int v)}])
                :maxLength  #(assoc % (v/sh-ns "maxLength") [{"@value" (platform/->int v)}])
                :minItems  (if (utils/array-shape? shape) #(assoc % (v/sh-ns "minCount") [{"@value" (platform/->int v)}]) identity)
                :maxItems  (if (utils/array-shape? shape) #(assoc % (v/sh-ns "maxCount") [{"@value" (platform/->int v)}]) identity)
                :pattern    #(assoc % (v/sh-ns "pattern")   [{"@value" v}])
                :format     #(assoc % (v/shapes-ns "format") [{"@value" v}])
                :additionalProperties #(assoc % (v/sh-ns "closed") [{"@value" (not (utils/->bool v))}])
                :x-uniqueItems #(assoc % (v/shapes-ns "uniqueItems") [{"@value" v}])
                :multipleOf #(assoc % (v/shapes-ns "multipleOf") [{"@value" (platform/->int v)}])
                :minimum    #(assoc % (v/sh-ns "minExclusive") [{"@value" (platform/->int v)}])
                :enum       #(assoc % (v/sh-ns "in") {"@list" (->> v (mapv utils/annotation->jsonld))})
                identity)))
       (reduce (fn [acc p] (p acc)) shape)
       (parse-generic-keywords node)))

(defn scalar-shape->property-shape [shape]
  {;; Object properties vs arrays, only one is allowed if it is an object (or scalar)
   (v/sh-ns "maxCount")  [{"@value" 1}]
   ;; instead of node, we have a datatype here
   (v/sh-ns "datatype")  (get shape (v/sh-ns "datatype"))})

(defn or-shape->property-shape [shape]
  (let [elements (utils/shacl-or-elements shape)
        elements (map #(dissoc % "@type") elements)]
    (-> shape
        (merge (utils/->shacl-or elements))
        (dissoc "@type")
        (assoc (v/sh-ns "maxCount")  [{"@value" 1}]))))

(defn array-shape->property-shape [shape]
  (let [items (get shape (v/shapes-ns "item"))
        min-count (utils/find-value shape (v/sh-ns "minCount"))
        max-count (utils/find-value shape (v/sh-ns "maxCount"))
        items (map (fn [shape]
                     (cond
                       (utils/or-shape? shape)     shape
                       (utils/scalar-shape? shape) {(v/sh-ns "datatype")  (get shape (v/sh-ns "datatype"))}
                       :else                       {(v/sh-ns "node")     [shape]}))
                   items)


        range (if (= 1 (count items))
                (first items)
                {(v/sh-ns "or") {"@list" items}})]
    (-> {;; we mark it for our own purposes, for example being able to detect
         ;; it easily without checking cardinality
         (v/shapes-ns "ordered") [{"@value" true}]
         (v/sh-ns "minCount") (if (some? min-count) [{"@value" min-count}] nil)
         (v/sh-ns "maxCount") (if (some? max-count) [{"@value" max-count}] nil)}
        (utils/clean-nils)
        (merge range))))

(defn node-shape->property-shape [shape]
  {;; Object properties vs arrays, only one is allowed if it is an object
   (v/sh-ns "maxCount")  [{"@value" 1}]
   ;; range of the prop
   (v/sh-ns "node")     [shape]})

(defn parse-shape [node {:keys [parsed-location] :as context}]
  (let [required-set (set (:required node []))
        properties (->> (:properties node [])
                        (mapv (fn [[k v]]
                               (let [parsed-location (str parsed-location "/property/" (utils/safe-str k))
                                     parsed-property-target (parse-type v (assoc context :parsed-location parsed-location))
                                     property-shape (cond
                                                      (utils/or-shape? parsed-property-target)     (or-shape->property-shape parsed-property-target)
                                                      (utils/scalar-shape? parsed-property-target) (scalar-shape->property-shape parsed-property-target)
                                                      (utils/array-shape? parsed-property-target)  (array-shape->property-shape parsed-property-target)
                                                      (utils/nil-shape? parsed-property-target)    (utils/nil-shape->property-shape)
                                                      :else (node-shape->property-shape parsed-property-target))
                                     property-shape (if (nil? (utils/find-value property-shape (v/sh-ns "minCount")))
                                                      ;; mandatory prop?
                                                      (assoc property-shape (v/sh-ns "minCount") [(if (required-set (utils/safe-str k)) {"@value" 1} {"@value" 0})])
                                                      property-shape)
                                     ;; common properties
                                     property-shape (-> property-shape
                                                        (assoc "@id" parsed-location)
                                                        (assoc "@type" [(v/sh-ns "PropertyShape") (v/sh-ns "Shape")])
                                                        (assoc (v/sh-ns "path") [{"@id" (v/anon-shapes-ns (utils/safe-str k))}])
                                                        (assoc (v/shapes-ns "propertyLabel") [{"@value" (utils/safe-str k)}])
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
         (parse-type-constraints node))))

(defn parse-array [node {:keys [parsed-location] :as context}]
  (let [required-set (set (:required node []))
        items (flatten [(:items node [])])
        items (->> items
                   (mapv (fn [i shape] (parse-type shape (assoc context :parsed-location (str parsed-location "/items/" i))))
                        (range 0 (count items))))]
    (->> {"@type" [(v/shapes-ns "Array")
                   (v/sh-ns "Shape")]
          "@id" parsed-location
          (v/shapes-ns "item") items}
         (utils/clean-nils)
         (parse-type-constraints node))))

(defn parse-scalar [parsed-location scalar-type]
  (-> {"@id" parsed-location
       "@type" [(v/shapes-ns "Scalar") (v/sh-ns "Shape")]
       (v/sh-ns "datatype") (if (= "shapes:any" scalar-type)
                              nil
                              [{"@id" scalar-type}])}
      utils/clean-nils))

(defn parse-file [node context]
  (->> {"@type" [(v/shapes-ns "FileUpload")
                 (v/sh-ns "Shape")]
        (v/shapes-ns "fileType") (utils/map-values node :x-fileTypes)}
       utils/clean-nils
       (parse-type-constraints node)))

(defn label [type-reference remote-id]
  (if (some? type-reference)
    (or (-> type-reference :name)
        (-> type-reference domain/shape :name)
        (utils/hash-path remote-id))
    nil))

(defn check-inclusion [node {:keys [parse-ast parsed-location] :as context}]
  (let [parsed (parse-ast node context)
        location (syntax/<-location node)
        reference (-> context (get :references {}) (get location))
        label (label reference location)
        shape {"@id" parsed-location
               "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
               (v/shapes-ns "inherits") [{"@id" location}]
               }]
    (if (and (some? reference) (some? label))
      (assoc shape v/sorg:name [{"@value" label}])
      shape)))

(defn find-reference
  "References can be local or remote, we check all possibilities"
  [references type-string context]
  (let [file-reference (utils/path-join (:base-uri context) (utils/safe-str type-string))
        file-reference-keyword (keyword file-reference)
        type-string-keyword (keyword type-string)
        found (->> [type-string type-string-keyword file-reference file-reference-keyword]
                   (mapv #(get references %))
                   (filter some?)
                   first)]
    (if (some? found)
      found
      (->> references
           (filter (fn [[k _]] (string/ends-with? (utils/safe-str k) (utils/safe-str type-string))))
           (mapv (fn [[_ v]] v))
           first))))

(defn check-reference
  "Checks if a provided string points to one of the types defined at the APIDocumentation level"
  [type-string {:keys [references parsed-location base-uri] :as context}]

  (if-let [type-reference (find-reference references type-string context)]
    (let [label-value (or (-> type-reference :name)
                          (if (satisfies? domain/Type type-reference) (-> type-reference domain/shape :name) nil)
                          (utils/last-component type-string))]
      (cond
        (some? (:x-ahead-declaration type-reference)) {"@id" parsed-location
                                                       "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
                                                       v/sorg:name [{"@value" label-value}]
                                                       (v/shapes-ns "inherits") [{"@id" (:x-ahead-declaration type-reference)}]}

        (satisfies? document/Includes type-reference) {"@id" parsed-location
                                                       "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
                                                       v/sorg:name [{"@value" label-value}]
                                                       (v/shapes-ns "inherits") [{"@id" (document/target type-reference)}]}
        :else

        (let [remote-id (-> type-reference domain/shape (get "@id"))
              label-value (label type-reference remote-id)]
          {"@id" parsed-location
           v/sorg:name [{"@value" label-value}]
           "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
           (v/shapes-ns "inherits") [{"@id" remote-id}]})))

    ;; we always try to return a reference
    {"@id" parsed-location
     "@type" [(v/shapes-ns "NodeShape") (v/sh-ns "Shape")]
     v/sorg:name [{"@value" (utils/last-component type-string)}]
     (v/shapes-ns "inherits") [{"@id" (if (= 0 (string/index-of type-string "#"))
                                        (str base-uri type-string)
                                        type-string)}]}))

(defn check-inheritance [{:keys [source with]} {:keys [parsed-location location] :as context}]
  (let [child (parse-type source context)
        with (flatten [with])
        super (->> with
                   (mapv (fn [i with]
                          (parse-type with (-> context
                                               (assoc :parsed-location (utils/path-join parsed-location (str "with/" i)))
                                               (assoc :location (utils/path-join location (str "with/" i))))))
                        (range 0 (count with))))
        child-super (get child (v/shapes-ns "inherits") [])]
    (assoc child (v/shapes-ns "inherits") (concat child-super super))))


(defn parse-type [node {:keys [parsed-location] :as context}]
  (let [type-string (if (string? node) node (:type node))]
    (cond
      ;; single reference with replacement or the reference
      ;; a)
      (some? (syntax/<-data node)) (check-inclusion node context)
      ;; b)
      (some? (:$ref node))  (check-reference (:$ref node) context)

      ;; inheritance
      (some? (:x-merge node))  (check-inheritance (:x-merge node) context)

      ;; scalars, explicit arrays, explicit objects
      (string? type-string) (condp = type-string
                              "string"  (condp = (:x-rdf-type node)
                                          "xsd:time" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "time")))

                                          "xsd:dateTime" (parse-type-constraints node (parse-scalar parsed-location (v/xsd-ns "dateTime")))

                                          "shapes:datetime-only" (parse-type-constraints node (parse-scalar parsed-location (v/shapes-ns "datetime-only")))

                                          "xsd:date" (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "date")))

                                          "shapes:any" (parse-type-constraints node  (parse-scalar parsed-location (v/shapes-ns "any")))

                                          nil (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "string"))))

                              "float"   (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "float")))
                              "integer" (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "integer")))
                              "number"  (condp = (:x-rdf-type node)

                                          "xsd:float" (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "float")))

                                          "xsd:integer" (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "integer")))

                                          nil (parse-type-constraints node  {(v/sh-ns "or") {"@list" [(parse-scalar (utils/path-join parsed-location "or/integer")
                                                                                                                    (v/xsd-ns "integer"))
                                                                                                      (parse-scalar (utils/path-join parsed-location "or/float")
                                                                                                                    (v/xsd-ns "float"))]}
                                                                             "@type" [(v/shapes-ns "Scalar") (v/sh-ns "Shape")]
                                                                             (v/shapes-ns "is-number") [{"@value" true}]}))

                              "file"    (parse-type-constraints node (parse-file node context))

                              "boolean" (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "boolean")))
                              "null"    (utils/parse-nil-value context)
                              "object"  (parse-shape node context)
                              "array"   (parse-array node context)
                              nil)

      (some? (get node :properties)) (parse-shape node context)

      (some? (get node :items))      (parse-array node context)

      :else                 nil)))
