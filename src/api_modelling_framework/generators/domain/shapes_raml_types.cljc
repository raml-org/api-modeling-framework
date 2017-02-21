(ns api-modelling-framework.generators.domain.shapes-raml-types
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn parse-shape-dispatcher-fn [shape ctx]
  (cond
    (utils/has-class? shape (v/shapes-ns "Scalar")) (v/shapes-ns "Scalar")
    (utils/has-class? shape (v/shapes-ns "Array"))  (v/shapes-ns "Array")
    (utils/has-class? shape (v/sh-ns "Shape"))      (v/sh-ns "Shape")
    :else nil))

(defmulti parse-shape (fn [shape ctx] (parse-shape-dispatcher-fn shape ctx)))

(defn parse-generic-keywords [shape raml-type]
  (->> shape
       (map (fn [[p _]]
              (condp = p
                (v/hydra-ns "title")       #(assoc % :displayName (utils/extract-jsonld-literal shape (v/hydra-ns "title")))
                (v/hydra-ns "description") #(assoc % :description (utils/extract-jsonld-literal shape (v/hydra-ns "description")))
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)
       (utils/clean-nils)))

(defn parse-constraints [raml-type shape]
  (->> shape
       (map (fn [[p v]]
              (condp = p
                (v/sh-ns "minLength")       #(assoc % :minLength v)
                (v/sh-ns "maxLength")       #(assoc % :maxLength v)
                (v/sh-ns "pattern")         #(assoc % :pattern   v)
                (v/shapes-ns "uniqueItems") #(assoc % :uniqueItems v)
                identity)))
       (reduce (fn [acc p] (p acc)) raml-type)
       (parse-generic-keywords shape)
       (utils/clean-nils)))


(defmethod parse-shape (v/shapes-ns "Scalar") [shape context]
  (let [sh-type (-> shape
                    (get (v/sh-ns "dataType"))
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
    (parse-constraints raml-type shape)))


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


(defmethod parse-shape (v/sh-ns "Shape") [shape context]
  (let [additionalProperties (utils/extract-jsonld-literal shape (v/sh-ns "closed") #(not %))
        properties (->> (get shape (v/sh-ns "property") [])
                        (map (fn [property]
                               (let [label (utils/extract-jsonld-literal property (v/shapes-ns "propertyLabel"))
                                     required (utils/extract-jsonld-literal property (v/sh-ns "minCount") #(if (= % 0) false true))
                                     range (utils/extract-jsonld property (v/shapes-ns "range") #(parse-shape % context))
                                     raml-type (-> range
                                                   (assoc :required required)
                                                   utils/clean-nils)]
                                 [label raml-type])))
                        (into {}))]
    (-> {:type "object"
         :properties properties
         :additionalProperties additionalProperties}
        utils/clean-nils
        (parse-constraints shape))))

(defmethod parse-shape nil [_ _] nil)
