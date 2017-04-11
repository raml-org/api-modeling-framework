(ns api-modelling-framework.parser.document.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.common :as common]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.parser.domain.openapi :as domain-parser]
            [api-modelling-framework.parser.document.common :refer [make-compute-fragments]]
            [cemerick.url :as url]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn check-abstract [encoded node]
  (let [abstract (get node :x-abstract-node nil)]
    (if abstract
      (-> (cond
            (satisfies? domain/Operation encoded) (assoc encoded :method nil)
            (satisfies? domain/EndPoint encoded)  (assoc encoded :path   nil)
            (satisfies? domain/Response encoded)  (assoc encoded :status-code nil)
            :else                                 encoded)
          (assoc :abstract abstract))
      encoded)))

(defn parse-ast-dispatch-function [node context]
  (cond
    (and (some? (syntax/<-location node))
         (some? (syntax/<-fragment node))
         (some? (syntax/<-data node))
         (= "Swagger Library"
            (->> node syntax/<-data :swagger)))     :library

    (and (some? (syntax/<-location node))
         (some? (syntax/<-fragment node))
         (some? (->> node syntax/<-data :swagger))) :root

    (some? (syntax/<-location node))                :fragment

    (and (nil? (syntax/<-location node))
         (nil? (syntax/<-fragment node)))       (throw
                                                 (new #?(:clj Exception :cljs js/Error)
                                                      (str "Unsupported OpenAPI parsing unit, missing @location or @fragment information")))

    :else                                       nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defn process-libraries [node {:keys [location parsed-location] :as context}]
  (let [uses (:x-uses (syntax/<-data node) [])]
    (let [libraries (->> uses
                         (reduce (fn [acc library]
                                   (let [library (parse-ast library context)]
                                     (conj acc library)))
                                 []))
          declares (reduce (fn [acc library]
                           (merge acc
                                  (->> (document/declares library)
                                       (map (fn [declare]
                                              [(document/id declare)
                                               declare]))
                                       (filter some?)
                                       (into {}))))
                         {}
                         libraries)]
      {:libraries libraries
       :library-declarations declares})))

(defn process-uses-tags [node {:keys [location parsed-location] :as context}]
  (let [uses (:x-uses (syntax/<-data node) [])]
    (let [source-map-id (str parsed-location "/source-map/uses")
          tags (->> uses
                    (mapv (fn [library]
                            (let [library-location (or (syntax/<-location library) library)
                                  library-alias (-> library-location (string/split #"/") last)]
                              (document/->UsesLibraryTag source-map-id library-alias library-location)))))]
      [(document/->DocumentSourceMap source-map-id location tags [])])))

(defn process-annotations [node {:keys [base-uri location parsed-location] :as context}]
  (debug "Processing " (count (:x-annotationTypes node {})) " annotations")
  (let [location (utils/path-join location "/x-annotationTypes")
        nested-context (-> context (assoc :location location) (assoc :parsed-location (str base-uri "#")))]
    (->> (:x-annotationTypes node {})
         (reduce (fn [acc [annotation-name annotation-node]]
                   (let [encoded-annotation-name (url/url-encode (utils/safe-str annotation-name))
                         range (domain-parser/parse-ast  annotation-node (-> nested-context
                                                                             (assoc :parsed-location (utils/path-join parsed-location (str encoded-annotation-name "/shape")))
                                                                             (assoc :type-hint :type)))
                         description (document/description range)
                         range (assoc range :description nil)
                         name (or (:displayName annotation-node) (utils/safe-str annotation-name))
                         range (assoc range :name nil)
                         allowed-targets (->> [(:allowedTargets annotation-node [])]
                                              flatten
                                              (map utils/node-name->domain-uri)
                                              (filter some?))
                         id (v/anon-shapes-ns encoded-annotation-name)]
                     (assoc acc
                            (utils/safe-str annotation-name) (->> (domain/map->ParsedDomainPropertySchema {:id id
                                                                                                           :name name
                                                                                                           :description description
                                                                                                           :sources (common/generate-is-annotation-sources annotation-name id parsed-location)
                                                                                                           :domain allowed-targets
                                                                                                           :range range})
                                                                  (common/with-location-meta-from node)))))
                 {}))))

(defmethod parse-ast :root [node context]
  (let [location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing OpenAPI Document at " location)
        fragments (or (:fragments context) (atom {}))
        compute-fragments (make-compute-fragments fragments)
        {:keys [libraries library-declarations]} (process-libraries node context)
        ;; just tags here, the libraries have been processed just above
        uses-tags (process-uses-tags node context)
        ;; only use of this is that when we parse the encoded
        ;; element, we will not generate an annotation for this
        ;; library annotation, but the parser will already find it
        ;; in the map of annotations
        ;; Only doc-annotations are stored in the model.
        libraries-annotations (->> library-declarations
                                   (filter (fn [[_ declaration]]
                                             (common/annotation-reference? declaration)))
                                   (mapv (fn [annotation]
                                           [(document/name annotation) (assoc annotation :from-library true)]))
                                   (into {}))
        doc-annotations (process-annotations (syntax/<-data node) {:base-uri location
                                                                   :location (str location "#")
                                                                   :parsed-location (str location "#")})
        annotations (atom (merge doc-annotations libraries-annotations))

        ;; we parse traits and types and add the information into the context
        traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                   :parsed-location (str location "#")
                                                                   :base-uri location
                                                                   :fragments fragments
                                                                   :references library-declarations
                                                                   :annotations annotations
                                                                   :document-parser parse-ast})

        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :parsed-location (str location "#")
                                                                 :base-uri location
                                                                 :fragments fragments
                                                                 :references library-declarations
                                                                 :annotations annotations
                                                                 :document-parser parse-ast})
        declarations (merge traits types)
        encoded (domain-parser/parse-ast (syntax/<-data node) {:location (str location "#")
                                                               :base-uri location
                                                               :fragments fragments
                                                               :annotations annotations

                                                               :references (merge declarations library-declarations)
                                                               :document-parser parse-ast
                                                               :is-fragment false})
        annotations (->> @annotations vals (filter #(nil? (:from-library %))))]
    (-> (document/map->ParsedDocument (merge context
                                             {:id location
                                              :location location
                                              :base-uri location
                                              :encodes encoded
                                              :declares (concat (vals declarations) annotations)
                                              :references (compute-fragments
                                                           (concat (vals @fragments) libraries))
                                              :sources uses-tags
                                              :document-type "OpenAPI"}))
        (assoc :raw (get node (keyword "@raw"))))))


(defmethod parse-ast :library [node context]
  (let [location (syntax/<-location node)
        _ (debug "Parsing OpenAPI Library at " location)
        fragments (or (:fragments context) (atom {}))
        compute-fragments (make-compute-fragments fragments)
        {:keys [libraries library-declarations]} (process-libraries node context)
        ;; just tags here, the libraries have been processed just above
        uses-tags (process-uses-tags node context)
        ;; only use of this is that when we parse the encoded
        ;; element, we will not generate an annotation for this
        ;; library annotation, but the parser will already find it
        ;; in the map of annotations
        ;; Only doc-annotations are stored in the model.
        libraries-annotations (->> library-declarations
                                   (filter (fn [[_ declaration]]
                                             (common/annotation-reference? declaration)))
                                   (mapv (fn [annotation]
                                           [(document/name annotation) (assoc annotation :from-library true)]))
                                   (into {}))
        doc-annotations (process-annotations (syntax/<-data node) {:base-uri location
                                                                   :location (str location "#")
                                                                   :parsed-location (str location "#")})
        annotations (atom (merge doc-annotations libraries-annotations))
        ;; we parse traits and types and add the information into the context
        traits(domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                  :parsed-location (str location "#")
                                                                  :base-uri location
                                                                  :fragments fragments
                                                                  :references library-declarations
                                                                  :annotations annotations
                                                                  :document-parser parse-ast})
        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :parsed-location (str location "#")
                                                                 :base-uri location
                                                                 :fragments fragments
                                                                 :references library-declarations
                                                                 :annotations annotations
                                                                 :document-parser parse-ast})
        declarations (merge traits types)
        annotations (->> @annotations vals (filter #(nil? (:from-library %))))]
    (-> (document/map->ParsedModule (merge context
                                           {:id location
                                            :base-uri location
                                            :location location
                                            :declares (compute-fragments
                                                       (concat (vals declarations) (filter
                                                                                    #(nil? (:from-library %))
                                                                                    annotations)))
                                            :references (vals @fragments)
                                            :tags uses-tags
                                            :document-type "OpenAPI Library"}))
        (assoc :raw (get node (keyword "@raw"))))))

(defmethod parse-ast :fragment [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        context (assoc context :base-uri location)
        _ (debug "Parsing OpenAPI Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        compute-fragments (make-compute-fragments fragments)
        {:keys [libraries library-declarations]} (process-libraries node context)
        ;; just tags here, the libraries have been processed just above
        uses-tags (process-uses-tags node context)
        references (or (:references context) {})
        libraries-annotations (->> library-declarations
                                   (filter (fn [[_ declaration]]
                                             (common/annotation-reference? declaration)))
                                   (mapv (fn [annotation]
                                           [(document/name annotation) annotation]))
                                   (into {}))
        annotations (atom libraries-annotations)
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge context
                                                                     {:location (str location "#")
                                                                      :base-uri location
                                                                      :fragments fragments
                                                                      :annotations annotations
                                                                      :references (merge references library-declarations)
                                                                      :document-parser parse-ast
                                                                      :is-fragment false}))
        encoded (check-abstract encoded (syntax/<-data node))]
    (-> (document/map->ParsedFragment {:id location
                                       :location location
                                       :base-uri location
                                       :encodes encoded
                                       :references (compute-fragments
                                                    (concat (vals @fragments) libraries))
                                       :sources uses-tags
                                       :document-type "OpenApi Fragment"})
        (assoc :raw (get node (keyword "@raw"))))))
