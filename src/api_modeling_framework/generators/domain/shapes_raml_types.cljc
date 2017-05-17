(ns api-modeling-framework.generators.domain.shapes-raml-types
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.generators.domain.common :as common]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn simplify [type]
  (cond
    (and (map? type)
         (some? (:properties type))) (dissoc type :type)

    (and (map? type)
         (= [:type] (keys type)))    (:type type)

    :else                            type))

(defn ref-shape [shape ctx]
  (let [ref (common/ref-shape? shape ctx)
        is-type-tag (-> ref
                        (document/find-tag document/is-type-tag)
                        first)
        type-name (cond
                    (some? is-type-tag)         (-> is-type-tag
                                                    (document/value)
                                                    keyword)
                    (some? (document/name ref)) (document/name ref)
                    :else                       (-> ref document/id (string/split #"/") last))]
    (utils/safe-str type-name)))

(defn array-shape? [shape]
  (utils/has-class? shape (v/shapes-ns "Array")))

(defn parse-shape-dispatcher-fn [shape ctx]
  (cond
    (utils/or-shape? shape)                                (v/sh-ns "or")
    (some? (get shape (v/shapes-ns "ramlTypeExpression"))) :raml-expression
    (some? (get shape (v/shapes-ns "inherits")))           :inheritance
    (utils/has-class? shape (v/shapes-ns "NilValueShape")) (v/shapes-ns "NilValueShape")
    (utils/has-class? shape (v/shapes-ns "Scalar"))        (v/shapes-ns "Scalar")
    (array-shape? shape)                                   (v/shapes-ns "Array")
    (utils/has-class? shape (v/shapes-ns "JSONSchema"))    (v/shapes-ns "JSONSchema")
    (utils/has-class? shape (v/shapes-ns "XMLSchema"))     (v/shapes-ns "XMLSchema")
    (utils/has-class? shape (v/sh-ns "NodeShape"))         (v/sh-ns "NodeShape")
    (utils/has-class? shape (v/shapes-ns "FileUpload"))    (v/shapes-ns "FileUpload")
    :else nil))


(defmulti parse-shape (fn [shape ctx] (parse-shape-dispatcher-fn shape ctx)))

(defn parse-generic-keywords [shape raml-type]
  (->> shape
       (map (fn [[p _]]
              (condp = p
                v/sorg:name        #(assoc % :displayName (utils/extract-jsonld-literal shape v/sorg:name))
                v/sorg:description #(assoc % :description (utils/extract-jsonld-literal shape v/sorg:description))
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)))

(defn parse-constraints [raml-type shape]
  (->> shape
       (map (fn [[p v]]
              (condp = p
                (v/sh-ns "minLength")       #(assoc % :minLength (get (first v) "@value"))
                (v/sh-ns "maxLength")       #(assoc % :maxLength (get (first v) "@value"))
                (v/sh-ns "minCount")      (if (array-shape? shape) #(assoc % :minItems (get (first v) "@value")) identity)
                (v/sh-ns "maxCount")      (if (array-shape? shape) #(assoc % :maxItems (get (first v) "@value")) identity)
                (v/sh-ns "pattern")         #(assoc % :pattern   (get (first v) "@value"))
                (v/sh-ns "closed")          #(assoc % :additionalProperties (not (utils/->bool (get (first v) "@value"))))
                (v/shapes-ns "uniqueItems") #(assoc % :uniqueItems (get (first v) "@value"))
                (v/sh-ns "minExclusive")       #(assoc % :minimum (get (first v) "@value"))
                (v/shapes-ns "multipleOf")     #(assoc % :multipleOf (get (first v) "@value"))
                (v/sh-ns "in")              #(assoc % :enum (map utils/jsonld->annotation (get v "@list" [])))
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)
       (parse-generic-keywords shape)
       (utils/clean-nils)))


(defmethod parse-shape (v/sh-ns "NodeShape") [shape context]
  (let [additionalProperties (utils/extract-jsonld-literal shape (v/sh-ns "closed") #(not %))
        properties (->> (get shape (v/sh-ns "property") [])
                        (map (fn [property]
                               (let [label (utils/extract-jsonld-literal property (v/shapes-ns "propertyLabel"))
                                     required (utils/extract-jsonld-literal property (v/sh-ns "minCount") #(if (= % 0) false true))
                                     range (cond
                                             (utils/array-range? property)   (parse-shape (utils/property-shape->array-shape property) context)
                                             (utils/or-shape? property)      (parse-shape (utils/property-shape->union-shape property) context)
                                             (utils/nil-range? property)     "nil"
                                             (utils/scalar-range? property)  (parse-shape (utils/property-shape->scalar-shape property) context)
                                             :else                     (parse-shape (utils/property-shape->node-shape property) context))
                                     range (if (string? range) {:type range} range)
                                     raml-type (-> range
                                                   (assoc :required (if (string/ends-with? label "?")
                                                                      (if required required nil)
                                                                      (if (= required false) required nil)))
                                                   utils/clean-nils)]
                                 [label (simplify raml-type)])))
                        (into {}))]
    (-> {:type "object"
         :properties properties
         :additionalProperties additionalProperties}
        utils/clean-nils
        (parse-constraints shape)
        simplify)))

(defmethod parse-shape (v/shapes-ns "Scalar") [shape context]
  (if (some? (get shape (v/shapes-ns "is-number")))
    (-> (parse-constraints {:type "number"} shape)
        simplify)
    (let [sh-type (-> shape
                      (get (v/sh-ns "datatype"))
                      first
                      (get "@id"))
          raml-type (condp = sh-type
                      (v/xsd-ns "string")           {:type "string"}
                      (v/xsd-ns "float")            {:type "number"}
                      (v/xsd-ns "integer")          {:type "integer"}
                      (v/xsd-ns "boolean")          {:type "boolean"}
                      (v/shapes-ns "null")          {:type "null"}
                      (v/xsd-ns "time")             {:type "time-only"}
                      (v/xsd-ns "dateTime")         {:type "datetime"}
                      (v/shapes-ns "datetime-only") {:type "datetime-only"}
                      (v/xsd-ns "date")             {:type "date-only"}
                      (v/shapes-ns "any")           {:type "any"}
                      (throw (new #?(:clj Exception :cljs js/Error) (str "Unknown scalar data type " sh-type))))]
      (-> (parse-constraints raml-type shape)
          simplify))))


(defmethod parse-shape (v/shapes-ns "Array") [shape context]
  (let [sh-items-type (->> (get shape (v/shapes-ns "item"))
                           (map #(parse-shape % context)))
        array-type (if (= 1 (count sh-items-type))
                     {:type "array"
                      :items (first sh-items-type)}
                     {:type "array"
                      :items {:type "union"
                              :of sh-items-type}
                      (keyword "(is-tuple)") true})]
    (parse-constraints array-type shape)))

(defmethod parse-shape (v/shapes-ns "JSONSchema") [shape context]
  (let [value (utils/extract-jsonld-literal shape (v/shapes-ns "schemaRaw"))]
    value))

(defmethod parse-shape (v/shapes-ns "XMLSchema") [shape context]
  (let [value (utils/extract-jsonld-literal shape (v/shapes-ns "schemaRaw"))]
    value))

(defmethod parse-shape (v/shapes-ns "NilValueShape") [shape context]
  {:type "nil"})


(defmethod parse-shape (v/shapes-ns "FileUpload") [shape context]
  (let [fileTypes (->> (get shape (v/shapes-ns "fileType") [])
                       (map #(get % "@value")))
        fileTypes (if (= 1 (count fileTypes)) (first fileTypes) fileTypes)]
    (-> {:type "file"
         :fileTypes fileTypes}
        (utils/clean-nils)
        (parse-constraints shape))))

(defn include-shape? [type {:keys [fragments]}] (get fragments type))

(defn include-shape [type {:keys [fragments document-generator] :as context}]
  (let [fragment (include-shape? type context)]
    (if (some? fragment)
      (let [expanded (document-generator fragment context)]
        expanded)
      nil)))

(defmethod parse-shape :inheritance [shape context]
  (let [base  (parse-shape (dissoc shape (v/shapes-ns "inherits")) context)
        types (->> (get shape (v/shapes-ns "inherits"))
                   (mapv (fn [type]
                           (let [type-id (get type "@id")]
                             (cond
                               (common/ref-shape? type-id context)    (ref-shape type-id context)
                               (include-shape? type-id context)       (include-shape type-id context)
                               :else                               (parse-shape type context))))))]
    (if (= 1 (count types))
      (if (utils/object-no-properties? base)
        (first types)
        (assoc  base :type (first types)))
      {:type "union"
       :anyOf types})))

(defmethod parse-shape (v/sh-ns "or") [shape context]
  ;; @todo support unions
  (-> {:type "number"}
      (parse-constraints shape)
      simplify))

(defmethod parse-shape :raml-expression [shape _] (-> shape (get (v/shapes-ns "ramlTypeExpression")) first (get "@value")))

(defmethod parse-shape nil [_ _] nil)
