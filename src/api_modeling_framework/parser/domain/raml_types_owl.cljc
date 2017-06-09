(ns api-modeling-framework.parser.domain.raml-types-owl
  (:require [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.parser.domain.raml-types-shapes :as shapes]))

(comment
(declare parse-type)

(defn parse-file-type [node parse-file-type]
  (->> {"@type" [(v/shapes-ns "FileUpload")]
        v/rdfs:subclassOf v/owl:Class
        (v/shapes-ns "fileType") (utils/map-values node :fileTypes)}
       utils/clean-nils
       (parse-type-constraints node)))

(defn parse-well-known-type-string [type-ref node {:keys [parsed-location] :as context}]
  (cond
    ;; scalars
    (= type-ref "any") v/owl:Thing
    ;; file type
    (= type-ref "file") (parse-type-constraints node (parse-file-type node parse-file-type))
    ;; nil type
    (= type-ref "nil") (parse-type-constraints node (utils/parse-nil-value context))
    ;; object
    (= type-ref "object")  (parse-shape node context)
    ;; unions
    (= type-ref "union")   (parse-union node context)
    ;; json schema
    (and
     (string? type-ref)
     (string/starts-with? type-ref "{"))  (parse-json-node parsed-location type-ref)
    ;; xmls schema
    (and
     (string? type-ref)
     (string/starts-with? type-ref "<"))  (parse-xml-node parsed-location type-ref)

    :else nil))

(defn check-inclusion [node {:keys [parse-ast parsed-location] :as context}]
  (let [parsed (parse-ast node context)
        location (syntax/<-location node)]
    {"@id" parsed-location
     "@type" [v/owl:Class]
     v/rdfs:subclassOf [{"@id" location}]}))

(defn parse-type [node {:keys [parsed-location default-type references] :as context}]
  (-> (cond
        (nil? node)                  nil

        (some? (syntax/<-data node)) (check-inclusion node context)

        (string? node)               (or (parse-well-known-type-string node {:type node} context)
                                         (parse-type-reference-link node with-raml-type-expression context))

        (map? node)                  (let [type-ref (or (:type node) (:schema node) (or default-type "object"))]
                                       (cond
                                         ;; unions here
                                         (some? (:anyOf node))        (parse-union node context)
                                         ;; it is scalar, an array, regular object or JSON/XML types
                                         (shapes/well-known-type? type-ref)  (parse-well-known-type-string type-ref node context)
                                         ;; it is a link to something that is not a well known type: type expression, referference
                                         (utils/link-format? node)    (parse-type-reference-link type-ref with-raml-type-expression context)
                                         :else
                                         ;; inheritance, we have properties in this node a
                                         (check-inheritance (utils/ensure-type-property node) context)))

        :else                        nil)
      (ensure-raml-type-expression-info-added with-raml-type-expression)))

)
