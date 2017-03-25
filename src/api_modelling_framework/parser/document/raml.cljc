(ns api-modelling-framework.parser.document.raml
  (:require [clojure.string :as string]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.domain.raml :as domain-parser]
            [api-modelling-framework.generators.domain.common :as common]
            [api-modelling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) [debug]]))

(defn parse-ast-dispatch-function [node context]
  (cond
    (and (some? (syntax/<-location node))
         (some? (syntax/<-fragment node)))      (string/trim (syntax/<-fragment node))

    (some? (syntax/<-location node))            :fragment

    (and (nil? (syntax/<-location node))
         (nil? (syntax/<-fragment node)))       (do
                                                  (throw
                                                   (new #?(:clj Exception :cljs js/Error)
                                                          (str "Unsupported RAML parsing unit, missing @location or @fragment information"))))

    :else                                               nil))

(defmulti parse-ast (fn [node context] (parse-ast-dispatch-function node context)))

(defn process-library [node {:keys [location parsed-location] :as context}]
  (let [uses (:uses (syntax/<-data node) {})
        libraries (reduce (fn [acc [alias library]]
                            (let [declares (parse-ast library context)]
                              (assoc acc alias declares)))
                          {}
                          uses)
        declares (reduce (fn [acc [alias library]]
                           (merge acc
                                  (->> (document/declares library)
                                       (map (fn [declare]
                                              (let [is-type-tag (-> declare (document/find-tag document/is-type-tag) first)
                                                    is-trait-tag (-> declare (document/find-tag document/is-trait-tag) first)
                                                    is-annotation-tag (-> declare (document/find-tag document/is-annotation-tag) first)
                                                    declaration-alias (cond
                                                                        (some? is-type-tag) (-> is-type-tag (document/value))
                                                                        (some? is-trait-tag) (-> is-trait-tag (document/value))
                                                                        (some? is-annotation-tag) (-> is-annotation-tag (document/value))
                                                                        :else nil)]
                                                (if (some? declaration-alias)
                                                  [(keyword (utils/alias-chain (str (utils/safe-str alias) "." (utils/safe-str declaration-alias)) context))
                                                   declare]
                                                  nil))))
                                       (filter some?)
                                       (into {}))))
                         {}
                         libraries)]
    {:libraries libraries
     :library-declarations declares}))

(defn process-uses-tags [node {:keys [location parsed-location]}]
  (let [uses (:uses (syntax/<-data node) {})]
    (let [source-map-id (str parsed-location "/source-map/uses")
          tags (map (fn [[alias library]]
                      (document/->UsesLibraryTag source-map-id (utils/safe-str alias) (syntax/<-location library))) uses)]
      [(document/->DocumentSourceMap source-map-id location tags [])])))

(defmethod parse-ast "#%RAML 1.0" [node context]
  (let [location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing RAML Document at " location)
        fragments (or (:fragments context) (atom {}))
        fragments (or (:fragments context) (atom {}))
        ;; library declarations are needed to parse the model encoded into the RAML file but it will not be stored
        ;; in the model, we will just keep a reference to the library through the uses tags
        {:keys [libraries library-declarations]} (process-library node {:location (str location "#")
                                                                        :fragments fragments
                                                                        :document-parser parse-ast
                                                                        :parsed-location (str location "#/libraries")})
        uses-tags (process-uses-tags node context)
        libraries-annotation (->> library-declarations
                                  (filter (fn [declaration]
                                            (common/annotation-reference? declaration)))
                                  (mapv (fn [annotation]
                                          [(document/name annotation) annotation]))
                                  (into {}))
        doc-annotations (domain-parser/process-annotations (syntax/<-data node) {:base-uri location
                                                                                 :location (str location "#")
                                                                                 :parsed-location (str location "#/annotations")})
        annotations (merge library-declarations doc-annotations)
        ;; we parse traits and types and add the information into the context
        traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                   :fragments fragments
                                                                   :references library-declarations
                                                                   :document-parser parse-ast
                                                                   :annotations annotations
                                                                   :parsed-location (str location "#/declares")})
        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :fragments fragments
                                                                 :references library-declarations
                                                                 :annotations annotations
                                                                 :document-parser parse-ast
                                                                 :parsed-location (str location "#/declares")})
        declarations (merge traits types)
        encoded (domain-parser/parse-ast (syntax/<-data node) {:location (str location "#")
                                                               :fragments fragments
                                                               :parsed-location (str location "#")
                                                               :annotations annotations
                                                               :references (merge declarations library-declarations)
                                                               :document-parser parse-ast
                                                               :is-fragment false})]
    (-> (document/map->ParsedDocument {:id location
                                       :location location
                                       :encodes encoded
                                       :declares (concat (vals declarations)
                                                         (vals doc-annotations))
                                       :references (concat (vals @fragments)
                                                           (flatten (vals libraries)))
                                       :sources uses-tags
                                       :document-type "#%RAML 1.0"})
        (assoc :raw (get node (keyword "@raw"))))))

(defmethod parse-ast "#%RAML 1.0 Library" [node {:keys [alias-chain] :as context}]
  (let [location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing RAML Library at " location)
        fragments (or (:fragments context) (atom {}))
        {:keys [libraries library-declarations]} (process-library node (dissoc context :alias-chain))
        uses-tags (process-uses-tags node context)
        libraries-annotation (->> library-declarations
                                  (filter (fn [declaration]
                                            (common/annotation-reference? declaration)))
                                  (mapv (fn [annotation]
                                          [(document/name annotation) annotation]))
                                  (into {}))
        doc-annotations (domain-parser/process-annotations (syntax/<-data node) {:base-uri location
                                                                                 :location (str location "#")
                                                                                 :parsed-location (str location "#/annotations")
                                                                                 :references library-declarations})
        annotations (merge libraries-annotation doc-annotations)
        ;; we parse traits and types and add the information into the context
        traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                   :fragments fragments
                                                                   :references library-declarations
                                                                   :alias-chain alias-chain
                                                                   :document-parser parse-ast
                                                                   :annotations annotations
                                                                   :parsed-location (str location "#/declares")})
        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :fragments fragments
                                                                 :alias-chain alias-chain
                                                                 :references library-declarations
                                                                 :annotations annotations
                                                                 :document-parser parse-ast
                                                                 :parsed-location (str location "#/declares")})
        declarations (merge traits types)
        usage (:usage (syntax/<-data node))]
    (-> (document/map->ParsedModule (utils/clean-nils
                                     {:id location
                                      :location location
                                      :description usage
                                      :declares (concat (vals declarations) (vals doc-annotations))
                                      :references (concat (vals @fragments)
                                                          (flatten (vals libraries)))
                                      :sources uses-tags
                                      :document-type "#%RAML 1.0 Library"}))
        (assoc :raw (get node (keyword "@raw"))))))


(defmethod parse-ast "#%RAML 1.0 Trait" [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing RAML Trait Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        ;; @todo is this illegal?
        references (or (:references context) {})
        trait-data (syntax/<-data node)
        usage (:usage trait-data)
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge
                                                               context
                                                               {:location (str location "#")
                                                                :fragments fragments
                                                                :references references
                                                                :parsed-location (str location "#")
                                                                :document-parser parse-ast
                                                                :is-fragment true}))]
    (-> (document/map->ParsedFragment {:id location
                                       :description usage
                                       :location location
                                       :encodes encoded
                                       :references (vals @fragments)
                                       :document-type "#%RAML 1.0 Trait"})
        (assoc :raw (get node (keyword "@raw"))))))

(defmethod parse-ast "#%RAML 1.0 DataType" [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing RAML DataType Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        ;; @todo is this illegal?
        references (or (:references context) {})
        type-data (syntax/<-data node)
        usage (:usage type-data)
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge
                                                               context
                                                               {:location (str location "#")
                                                                :fragments fragments
                                                                :references references
                                                                :parsed-location (str location "#")
                                                                :document-parser parse-ast
                                                                :is-fragment true}))]
    (-> (document/map->ParsedFragment {:id location
                                       :description usage
                                       :location location
                                       :encodes encoded
                                       :references (vals @fragments)
                                       :document-type "#%RAML 1.0 DataType"})
        (assoc :raw (get node (keyword "@raw"))))))

(defn parse-fragment [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        fragments (or (:fragments context) (atom {}))
        ;; @todo is this illegal?
        references (or (:references context) {})
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge
                                                               context
                                                               {:location (str location "#")
                                                                :fragments fragments
                                                                :references references
                                                                :parsed-location (str location "#")
                                                                :document-parser parse-ast
                                                                :is-fragment true}))]
    (-> (document/map->ParsedFragment {:id location
                                       :location location
                                       :encodes encoded
                                       :references (vals @fragments)
                                       :document-type "#%RAML 1.0 Fragment"})
        (assoc :raw (get node (keyword "@raw"))))))

(defmethod parse-ast :fragment [node context]
  (parse-fragment node context))

(defmethod parse-ast "#%RAML 1.0 Fragment" [node context]
  (parse-fragment node context))
