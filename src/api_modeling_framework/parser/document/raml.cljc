(ns api-modeling-framework.parser.document.raml
  (:require [clojure.string :as string]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.parser.domain.raml :as domain-parser]
            [api-modeling-framework.parser.domain.common :as common]
            [api-modeling-framework.parser.document.common :refer [make-compute-fragments]]
            [api-modeling-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) [debug]]))


(def DOC_CACHE (atom {}))

(defn reset-cache [] (reset! DOC_CACHE {}))

(defn with-cache [node f]
  (f)
  ;;(let [location (syntax/<-location node)
  ;;      id (-> location (string/split #"/") last)]
  ;;  (println "CACHE CHECK " id)
  ;;  (if-let [parsed (get @DOC_CACHE id)]
  ;;    (do
  ;;      (prn "HIT! " id)
  ;;      parsed)
  ;;    (let [parsed (f)]
  ;;      (prn "MISS! "id)
  ;;      (swap! DOC_CACHE #(assoc % id parsed))
  ;;      parsed)))
  )

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

(defn process-libraries [node {:keys [location parsed-location] :as context}]
  (let [uses (:uses (syntax/<-data node) {})
        libraries (->> uses
                       (reduce (fn [acc [alias library]]
                                 (let [library (parse-ast library context)]
                                   (assoc acc alias library)))
                               {}))
        vocabularies (->> libraries
                          (filter (fn [[alias library]] (satisfies? document/Vocabulary library)))
                          (reduce (fn [acc [alias library]]
                                    (let [base (-> library document/vocabulary domain/base)]
                                      (assoc acc alias base)))
                                  {}))
        vocabularies {}
        declares (->> libraries
                      (filter (fn [[alias library]] (satisfies? document/Module library)))
                      (reduce (fn [acc [alias library]]
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
                                                        ;; we provide the full URI for the dependency
                                                        ;; we cannot use just the hash reference as in other
                                                        ;; declarations, because we need to resolve it remotely
                                                        declare]
                                                       nil))))
                                            (filter some?)
                                            (into {}))))
                              {}))]
    {:libraries libraries
     :library-declarations declares
     :vocabularies vocabularies}))


(defn process-uses-tags [node {:keys [location parsed-location]}]
  (let [uses (:uses (syntax/<-data node) {})]
    (let [source-map-id (str parsed-location "/source-map/uses")
          tags (map (fn [[alias library]]
                      (document/->UsesLibraryTag source-map-id (utils/safe-str alias) (syntax/<-location library))) uses)]
      [(document/->DocumentSourceMap source-map-id location tags [])])))

(defmethod parse-ast "#%RAML 1.0" [node context]
  (with-cache node
    #(let [location (syntax/<-location node)
           context (assoc context :base-uri location)
           fragments (or (:fragments context) (atom {}))
           compute-fragments (make-compute-fragments fragments)
           ;; library declarations are needed to parse the model encoded into the RAML file but it will not be stored
           ;; in the model, we will just keep a reference to the library through the uses tags
           {:keys [libraries library-declarations]} (process-libraries node {:location (str location "#")
                                                                             :fragments fragments
                                                                             :document-parser parse-ast
                                                                             :parsed-location (str location "#/libraries")})
           ;; just tags here, the libraries have been processed just above
           uses-tags (process-uses-tags node context)
           libraries-annotation (->> library-declarations
                                     (filter (fn [declaration]
                                               (common/annotation-reference? declaration)))
                                     (mapv (fn [annotation]
                                             [(document/name annotation) annotation]))
                                     (into {}))
           working-declarations library-declarations
           ;; we parse traits and types and add the information into the context
           types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                    :fragments fragments
                                                                    :references working-declarations
                                                                    :document-parser parse-ast
                                                                    :parsed-location location})
           working-declarations (merge working-declarations types)
           doc-annotations (domain-parser/process-annotations (syntax/<-data node) {:base-uri location
                                                                                    :references working-declarations
                                                                                    :location (str location "#")
                                                                                    :parsed-location (str location "#")})
           annotations (merge library-declarations doc-annotations)
           traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                      :fragments fragments
                                                                      :references working-declarations
                                                                      :document-parser parse-ast
                                                                      :annotations annotations
                                                                      :parsed-location (str location "#")})
           working-declarations (merge working-declarations traits)
           encoded (domain-parser/parse-ast (syntax/<-data node) {:location (str location "#")
                                                                  :fragments fragments
                                                                  :parsed-location (str location "#")
                                                                  :annotations annotations
                                                                  :references working-declarations
                                                                  :document-parser parse-ast
                                                                  :is-fragment false})]
       (-> (document/map->ParsedDocument {:id location
                                          :location location
                                          :encodes encoded
                                          :declares (concat (vals types)
                                                            (vals traits)
                                                            (vals doc-annotations))
                                          :references (compute-fragments
                                                       (concat (vals @fragments)
                                                               (flatten (vals libraries))))
                                          :sources uses-tags
                                          :document-type "#%RAML 1.0"})
           (assoc :raw (get node (keyword "@raw")))))))

(defmethod parse-ast "#%RAML 1.0 Library" [node {:keys [alias-chain] :as context}]
  (with-cache node
    #(let [location (syntax/<-location node)
          context (assoc context :base-uri location)
          _ (debug "Parsing RAML Library at " location)
          fragments (or (:fragments context) (atom {}))
          compute-fragments (make-compute-fragments fragments)
          {:keys [libraries library-declarations]} (process-libraries node (dissoc context :alias-chain))
          uses-tags (process-uses-tags node context)
          libraries-annotation (->> library-declarations
                                    (filter (fn [declaration]
                                              (common/annotation-reference? declaration)))
                                    (mapv (fn [annotation]
                                            [(document/name annotation) annotation]))
                                    (into {}))
          working-declarations library-declarations
          ;; we parse traits and types and add the information into the context
          types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                   :fragments fragments
                                                                   :alias-chain alias-chain
                                                                   :references working-declarations
                                                                   :document-parser parse-ast
                                                                   :parsed-location location})
          working-declarations (merge working-declarations types)
          doc-annotations (domain-parser/process-annotations (syntax/<-data node) {:base-uri location
                                                                                   :location (str location "#")
                                                                                   :parsed-location (str location "#/annotations")
                                                                                   :references working-declarations})
          annotations (merge libraries-annotation doc-annotations)
          traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                     :fragments fragments
                                                                     :references working-declarations
                                                                     :alias-chain alias-chain
                                                                     :document-parser parse-ast
                                                                     :annotations annotations
                                                                     :parsed-location (str location "#")})

          usage (:usage (syntax/<-data node))]
       (-> (document/map->ParsedModule (utils/clean-nils
                                       {:id location
                                        :location location
                                        :description usage
                                        :declares (concat (vals types)
                                                          (vals traits)
                                                          (vals doc-annotations))
                                        :references (compute-fragments
                                                     (concat (vals @fragments)
                                                             (flatten (vals libraries))))
                                        :sources uses-tags
                                        :document-type "#%RAML 1.0 Library"}))
          (assoc :raw (get node (keyword "@raw")))))))

(defn parse-fragment
  ([node context fragment-type]
   (with-cache node
     #(let [context (or context {})
            location (syntax/<-location node)
            context (assoc context :base-uri location)
            _ (debug "Parsing " fragment-type " Fragment at " location)
            fragments (or (:fragments context) (atom {}))
            compute-fragments (make-compute-fragments fragments)
            ;; library declarations are needed to parse the model encoded into the RAML file but it will not be stored
            ;; in the model, we will just keep a reference to the library through the uses tags
            {:keys [libraries library-declarations]} (process-libraries node {:location (str location "#")
                                                                              :fragments fragments
                                                                              :document-parser parse-ast
                                                                              :parsed-location (str location "#/libraries")})
            libraries-annotation (->> library-declarations
                                      (filter (fn [declaration]
                                                (common/annotation-reference? declaration)))
                                      (mapv (fn [annotation]
                                              [(document/name annotation) annotation]))
                                      (into {}))

            uses-tags (process-uses-tags node context)
            document-tags (document/generate-document-sources location fragment-type)

            ;; @todo is this illegal?
            references (or (:references context) {})
            fragment-data (syntax/<-data node)
            usage(:usage fragment-data)

            encoded (domain-parser/parse-ast fragment-data (merge
                                                            context
                                                            {:location (str location "#")
                                                             :fragments fragments
                                                             :references (merge references library-declarations)
                                                             :annotations libraries-annotation
                                                             :parsed-location (str location "#")
                                                             :type-hint (condp = fragment-type
                                                                          "#%RAML 1.0 Trait" :method
                                                                          "#%RAML 1.0 DataType" :type
                                                                          ;; the type hint might have be passed by the calling parser
                                                                          (:type-hint context))
                                                             :document-parser parse-ast
                                                             :is-fragment false}))]
        (-> (document/map->ParsedFragment {:id location
                                           :description usage
                                           :location location
                                           :encodes encoded
                                           :references (compute-fragments
                                                        (concat (vals @fragments)
                                                                (flatten (vals libraries))))
                                           :sources (concat uses-tags document-tags)
                                           :document-type fragment-type})
            (assoc :raw (get node (keyword "@raw")))))))
  ([node context] (parse-fragment node context "#%RAML 1.0 Fragment")))

(defn parse-vocabulary [node context fragment-type]
  (let [location (syntax/<-location node)
        document-tags (document/generate-document-sources location fragment-type)
        vocabulary-data(syntax/<-data node)
        usage (:usage vocabulary-data)
        uses-tags (process-uses-tags node context)
        {:keys [vocabularies libraries]} (process-libraries node {:location (str location "#")
                                                                  :document-parser parse-ast
                                                                  :parsed-location (str location "#/libraries")})
        externals (common/ast-get vocabulary-data :external {})
        vocabularies (->>  (merge vocabularies externals)
                           (map (fn [[k v]] [(utils/safe-str k) (utils/safe-str v)]))
                           (into {}))]
    (-> (document/map->ParsedVocabulary {:id location
                                             :location location
                                             :description usage
                                             :sources (concat uses-tags document-tags)
                                             :references (flatten (vals libraries))
                                             :externals externals
                                             :vocabulary (domain-parser/parse-ast vocabulary-data
                                                                                  (merge context
                                                                                         {:location (str location "#")
                                                                                          :document-parser parse-ast
                                                                                          :type-hint :vocabulary
                                                                                          :vocabularies vocabularies
                                                                                          :is-fragment false}))})
                (assoc :raw (get node (keyword "@raw"))))))

(defn make-abstract-trait [domain]
  (let [encoded (document/encodes domain)
        encoded (-> encoded
                    (assoc :method nil)
                    (assoc :abstract true))]
    (assoc domain :encodes encoded)))

(defn check-abstract [model data]
  (if (and (some? model)
           (satisfies? domain/DomainElement model))
    (assoc model :abstract (get data (keyword "(abstract)") false))
    model))

(defmethod parse-ast "#%RAML 1.0 Vocabulary" [node context]
  (parse-vocabulary node context "#%RAML 1.0 Vocabulary"))

(defmethod parse-ast "#%RAML 1.0 DataType" [node context]
  (parse-fragment node context "#%RAML 1.0 DataType"))

(defmethod parse-ast "#%RAML 1.0 Trait" [node context]
  (make-abstract-trait (parse-fragment node context "#%RAML 1.0 Trait")))

(defmethod parse-ast "#%RAML 1.0 Fragment" [node context]
  (check-abstract (parse-fragment node context) (syntax/<-data node)))

(defmethod parse-ast :fragment [node context]
  (check-abstract (parse-fragment node context) (syntax/<-data node)))
