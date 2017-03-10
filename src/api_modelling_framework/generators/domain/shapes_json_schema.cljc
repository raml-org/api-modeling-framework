(ns api-modelling-framework.generators.domain.shapes-json-schema
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.generators.domain.common :as common]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn ref-shape [shape ctx]
  (let [ref (common/ref-shape? shape ctx)
        is-type-tag (-> ref
                        (document/find-tag document/is-type-tag)
                        first)
        type-name (if (some? is-type-tag)
                    (-> is-type-tag
                        (document/value)
                        keyword)
                    (-> ref document/id (string/split #"/") last))
        type-name (utils/safe-str type-name)]
    (if (:from-library ref)
      {:$ref (document/id ref)}
      (if (string/starts-with? type-name "#")
        {:$ref type-name}
        {:$ref (str "#/definitions/" type-name)}))))

(defn parse-shape-dispatcher-fn [shape ctx]
  (cond
    (nil? shape)            nil
    (some? (get shape (v/shapes-ns "inherits")))        :inheritance
    (utils/has-class? shape (v/shapes-ns "Scalar"))     (v/shapes-ns "Scalar")
    (utils/has-class? shape (v/shapes-ns "Array"))      (v/shapes-ns "Array")
    (utils/has-class? shape (v/sh-ns "Shape"))          (v/sh-ns "Shape")
    (utils/has-class? shape (v/shapes-ns "JSONSchema")) (v/sh-ns "JSONSchema")
    (utils/has-class? shape (v/shapes-ns "XMLSchema"))  (v/sh-ns "XMLSchema")
    :else nil))

(defmulti parse-shape (fn [shape ctx] (parse-shape-dispatcher-fn shape ctx)))

(defn parse-constraints [raml-type shape]
  (->> shape
       (map (fn [[p v]]
              (condp = p
                (v/sh-ns "minLength")       #(assoc % :minLength (get (first v) "@value"))
                (v/sh-ns "maxLength")       #(assoc % :maxLength (get (first v) "@value"))
                (v/sh-ns "pattern")         #(assoc % :pattern   (get (first v) "@value"))
                (v/shapes-ns "uniqueItems") #(assoc % :uniqueItems (get (first v) "@value"))
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)))

(defmethod parse-shape (v/sh-ns "Shape") [shape context]
  (let [additionalProperties (utils/extract-jsonld-literal shape (v/sh-ns "closed") #(not %))
        required-props (atom [])
        properties (->> (get shape (v/sh-ns "property") [])
                        (map (fn [property]
                               (let [label (utils/extract-jsonld-literal property (v/shapes-ns "propertyLabel"))
                                     required (utils/extract-jsonld-literal property (v/sh-ns "minCount") #(if (= % 0) false true))
                                     range (utils/extract-jsonld property (v/shapes-ns "range") #(parse-shape % context))
                                     range (if (string? range) {:type range} range)]
                                 (when required (swap! required-props #(concat % [label])))
                                 [label (or range {})])))
                        (into {}))]
    (-> {:type "object"
         :properties properties
         :required (if (empty? @required-props) nil @required-props)
         :additionalProperties additionalProperties}
        utils/clean-nils
        (parse-constraints shape))))

(defmethod parse-shape (v/shapes-ns "Array") [shape context]
  (let [sh-items-type (->> (get shape (v/shapes-ns "item"))
                           (map #(parse-shape % context)))
        sh-items-type (if (= 1 (count sh-items-type)) (first sh-items-type) sh-items-type)]
    (parse-constraints {:type "array"
                        :items sh-items-type} shape)))

(defmethod parse-shape (v/shapes-ns "Scalar") [shape context]
  (let [sh-type (-> shape
                    (get (v/sh-ns "dataType"))
                    first
                    (get "@id"))
        raml-type (condp = sh-type
                    (v/xsd-ns "string")           {:type "string"}
                    (v/xsd-ns "float")            {:type "number"}
                    (v/xsd-ns "integer")          {:type "number"
                                                   :x-rdf-type "xsd:integer"}
                    (v/xsd-ns "boolean")          {:type "boolean"}
                    (v/shapes-ns "null")          {:type "null"}
                    (v/xsd-ns "time")             {:type "string"
                                                   :x-rdf-type "xsd:time"}
                    (v/xsd-ns "dateTime")         {:type "string"
                                                   :x-rdf-type "xsd:dateTime"}
                    (v/shapes-ns "datetime-only") {:type "string"
                                                   :x-rdf-type "shapes:datetime-only"}
                    (v/xsd-ns "date")             {:type "string"
                                                   :x-rdf-type "xsd:date"}
                    (v/shapes-ns "any")           {:type "string"
                                                   :x-rdf-type "shapes:any"}
                    (throw (new #?(:clj Exception :cljs js/Error) (str "Unknown scalar data type " sh-type))))]
    (parse-constraints raml-type shape)))

(defmethod parse-shape (v/sh-ns "JSONSchema") [shape context]
  (let [value (utils/extract-jsonld-literal shape (v/shapes-ns "schemaRaw"))]
    {:type "EmbeddedJSONSchema"
     :value value}))

(defmethod parse-shape (v/sh-ns "XMLSchema") [shape context]
  (let [value (utils/extract-jsonld-literal shape (v/shapes-ns "schemaRaw"))]
    {:type "EmbeddedXMLSchema"
     :value value}))

(defmethod parse-shape :inheritance [shape context]
  (let [types (->> (get shape (v/shapes-ns "inherits"))
                   (mapv (fn [type]
                           (if (common/ref-shape? type context)
                             (ref-shape type context)
                             (parse-shape type context)))))]
    (if (= 1 (count types))
      (first types)
      {:x-merge types})))

(defmethod parse-shape nil [_ _] {})
