(ns api-modeling-framework.parser.domain.common
  (:require [api-modeling-framework.model.document :as document]
            [api-modeling-framework.utils :as utils]
            [clojure.string :as string]))

(defn generate-is-type-sources [type-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/is-type")
        is-type-tag (document/->IsTypeTag source-map-id type-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [is-type-tag] [])]))

(defn type-reference [location type-name]
  (let [hash-fragment (if (string/ends-with? location "#")
                        "/definitions"
                        "#/definitions")]
    (str location (utils/path-join hash-fragment type-name))))


(defn with-location-meta-from [n m]
  (if (meta n)
    (assoc m :lexical (meta n))
    m))

(defn generate-is-annotation-sources [annotation-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/is-annotation")
        is-trait-tag (document/->IsAnnotationTag source-map-id annotation-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [is-trait-tag] [])]))

(defn annotation-reference? [model]
  (-> model
      (document/find-tag document/is-annotation-tag)
      first
      some?))
