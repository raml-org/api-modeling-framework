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

(defn wrapped-ast-token? [node]
  (not= :amf-not-found (:amf-lexical-token node :amf-not-found)))

;; would have been nice to use a macro here, but clojurescript doesn't work
;; nicely with them
(defn with-ast-parsing [ast-node f]
  (if-let [actual-node (and (map? ast-node)
                            (:amf-lexical-token ast-node))]
    (with-location-meta-from ast-node (f actual-node))
    (with-location-meta-from ast-node (f ast-node))))

(defn ast-value [node]
  (if (wrapped-ast-token? node)
    (:amf-lexical-token node)
    node))

(defn ast-assoc
  ([m k v]
   (assoc m k (ast-value v))))

(defn ast-get
  ([m k]
   (ast-value (get m k)))
  ([m k default] (ast-value (get m k default))))


(defn purge-ast [x]
  (cond
    (and (map? x)
         (wrapped-ast-token? x)) (ast-value x)
    (map? x)                     (->> x
                                      (mapv (fn [[k v]] [k (purge-ast (ast-value v))]))
                                      (into {}))
    (coll? x)                    (mapv purge-ast x)
    :else                        (ast-value x)))
