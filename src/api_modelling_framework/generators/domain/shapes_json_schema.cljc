(ns api-modelling-framework.generators.domain.shapes-json-schema
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn parse-shape-dispatcher-fn [shape ctx]
  (cond
    (utils/has-class? shape (v/shapes-ns "Scalar")) (v/shapes-ns "Scalar")
    (utils/has-class? shape (v/shapes-ns "Array")) (v/shapes-ns "Array")
    (utils/has-class? shape (v/sh-ns "Shape"))      (v/sh-ns "Shape")
    :else nil))

(defmulti parse-shape (fn [shape ctx] (parse-shape-dispatcher-fn shape ctx)))

(defn parse-constraints [raml-type shape]
  (->> shape
       (map (fn [[p v]]
              (condp = p
                (v/sh-ns "minLength")       #(assoc % :minLength v)
                (v/sh-ns "maxLength")       #(assoc % :maxLength v)
                (v/sh-ns "pattern")         #(assoc % :pattern   v)
                (v/shapes-ns "uniqueItems") #(assoc % :uniqueItems v)
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)))

(defmethod parse-shape (v/sh-ns "Shape") [shape context]
  (let [additionalProperties (utils/extract-jsonld-literal shape (v/sh-ns "closed") #(not %))
        required-props (atom [])
        properties (->> (get shape (v/sh-ns "property") [])
                        (map (fn [property]
                               (let [label (utils/extract-jsonld-literal property (v/shapes-ns "propertyLabel"))
                                     required (utils/extract-jsonld-literal property (v/sh-ns "minCount") #(if (= % 0) false true))
                                     raml-type(utils/extract-jsonld property (v/shapes-ns "range") #(parse-shape % context))]
                                 (when required (swap! required-props #(concat % [label])))
                                 [label raml-type])))
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
                    (v/xsd-ns "date")             {:type "sting"
                                                   :x-rdf-type "xsd:date"}
                    (v/shapes-ns "any")           {:type "string"
                                                   :x-rdf-type "shapes:any"}
                    (throw (new #?(:clj Exception :cljs js/Error) (str "Unknown scalar data type " sh-type))))]
    (parse-constraints raml-type shape)))

(defmethod parse-shape nil [_ _] nil)
