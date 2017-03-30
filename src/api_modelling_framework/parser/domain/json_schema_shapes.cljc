(ns api-modelling-framework.parser.domain.json-schema-shapes
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(declare parse-type)

(defn parse-generic-keywords [node shape]
  (->> node
       (map (fn [[p v]]
              (condp = p
                :title #(assoc % (v/hydra-ns "title") [{"@value" v}])
                :description #(assoc % (v/hydra-ns "description") [{"@value" v}])
                identity)))
       (reduce (fn [acc p] (p acc)) shape)))
(defn parse-type-constraints [node shape]
  (->> node
       (map (fn [[p v]]
              (condp = p
                :minLength  #(assoc % (v/sh-ns "minLength") [{"@value" v}])
                :maxLength  #(assoc % (v/sh-ns "maxLength") [{"@value" v}])
                :pattern    #(assoc % (v/sh-ns "pattern")   [{"@value" v}])
                :format     #(assoc % (v/shapes-ns "format") [{"@value" v}])
                :multipleOf #(assoc % (v/shapes-ns "multipleOf") [{"@value" v}])
                :minimum    #(assoc % (v/sh-ns "minExclusive") [{"@value" v}])
                identity)))
       (reduce (fn [acc p] (p acc)) shape)
       (parse-generic-keywords node)))

(defn parse-shape [node {:keys [parsed-location] :as context}]
  (let [parsed-location (str parsed-location "/shape")
        required-set (set (:required node []))
        properties (->> (:properties node [])
                        (map (fn [[k v]]
                               (let [parsed-location (str parsed-location "/property/" k)]
                                 (->> {"@type" ["sh:PropertyConstraint"]
                                       "@id" parsed-location
                                       (v/shapes-ns "propertyLabel") [{"@value" (name k)}]
                                       ;; mandatory prop?
                                       (v/sh-ns "minCount") [(if (required-set (name k)) {"@value" 1} {"@value" 0})]
                                       ;; range of the prop
                                       (v/shapes-ns "range") [(parse-type v (assoc context :parsed-location parsed-location))]}
                                      (parse-type-constraints v))))))
        open-shape (:additionalProperties node)]
    (->> {"@type" [(v/sh-ns "Shape")]
          "@id" parsed-location
          (v/sh-ns "property") properties
          (v/sh-ns "closed") (if (some? open-shape)
                               [{"@value" (not open-shape)}]
                               nil)}
         utils/clean-nils
         (parse-type-constraints node))))

(defn parse-array [node {:keys [parsed-location] :as context}]
  (let [parsed-location (str parsed-location "/shape")
        required-set (set (:required node []))
        items (->> [(:items node {})]
                   flatten
                   (map (fn [shape] (parse-type shape (assoc context :parsed-location parsed-location)))))]
    (->> {"@type" [(v/sh-ns "Shape")
                   (v/shapes-ns "Array")]
          "@id" parsed-location
          (v/shapes-ns "item") items}
         (parse-type-constraints node))))

(defn parse-scalar [parsed-location scalar-type]
  {"@id" (str parsed-location "/shape")
   "@type" [(v/sh-ns "Shape") (v/shapes-ns "Scalar")]
   (v/sh-ns "dataType") [{"@id" scalar-type}]})


(defn parse-type [node {:keys [parsed-location] :as context}]
  (let [type-string (if (string? node) node (:type node))
        shape (condp = type-string
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

                            nil (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "float"))))

                "boolean"                       (parse-type-constraints node  (parse-scalar parsed-location (v/xsd-ns "boolean")))
                "null"                          (parse-type-constraints node  (parse-scalar parsed-location (v/shapes-ns "null")))
                "object"                        (parse-shape node context)
                "array"                         (parse-array node context)
                (if (some? (get node :properties))
                  (parse-shape node context)
                  nil))]
    shape))
