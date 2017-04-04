(ns api-modelling-framework.parser.document.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.openapi :as domain-parser]
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

(defn process-uses-tags [node {:keys [location parsed-location] :as context}]
  (let [uses (:x-uses (syntax/<-data node) [])]
    (let [source-map-id (str parsed-location "/source-map/uses")
          libraries (->> uses
                         (reduce (fn [acc library]
                                   (let [library (parse-ast library context)]
                                     (conj acc library)))
                                 []))
          tags (->> uses
                    (mapv (fn [library]
                            (let [library-location (or (syntax/<-location library) library)
                                  library-alias (-> library-location (string/split #"/") last)]
                              (document/->UsesLibraryTag source-map-id library-alias library-location)))))]
      [libraries [(document/->DocumentSourceMap source-map-id location tags [])]])))

(defmethod parse-ast :root [node context]
  (let [location (syntax/<-location node)
        _ (debug "Parsing OpenAPI Document at " location)
        fragments (or (:fragments context) (atom {}))
        [libraries uses-tags] (process-uses-tags node context)
        annotations (atom {})
        ;; we parse traits and types and add the information into the context
        traits (domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                   :base-uri location
                                                                   :fragments fragments
                                                                   :annotations annotations
                                                                   :document-parser parse-ast})

        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :base-uri location
                                                                 :fragments fragments
                                                                 :annotations annotations
                                                                 :document-parser parse-ast})
        declarations (merge traits types)

        encoded (domain-parser/parse-ast (syntax/<-data node) {:location (str location "#")
                                                               :base-uri location
                                                               :fragments fragments
                                                               :annotations annotations
                                                               :references declarations
                                                               :document-parser parse-ast
                                                               :is-fragment false})]
    (-> (document/map->ParsedDocument (merge context
                                             {:id location
                                              :location location
                                              :base-uri location
                                              :encodes encoded
                                              :declares (concat (vals declarations) (vals @annotations))
                                              :references (concat (vals @fragments) libraries)
                                              :sources uses-tags
                                              :document-type "OpenAPI"}))
        (assoc :raw (get node (keyword "@raw"))))))

(defmethod parse-ast :fragment [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        _ (debug "Parsing OpenAPI Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        [libraries uses-tags] (process-uses-tags node context)
        libraries-declarations (->> libraries
                                    (map (fn [lib] (document/declares lib)))
                                    flatten
                                    (map (fn [dec] [(document/id dec) dec]))
                                    (into {}))
        references (or (:references context) {})
        annotations (atom {})
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge context
                                                                     {:location (str location "#")
                                                                      :base-uri location
                                                                      :fragments fragments
                                                                      :annotations annotations
                                                                      :references (merge references libraries-declarations)
                                                                      :document-parser parse-ast
                                                                      :is-fragment false}))
        encoded (check-abstract encoded (syntax/<-data node))]
    (-> (document/map->ParsedFragment {:id location
                                       :location location
                                       :base-uri location
                                       :encodes encoded
                                       :references (concat (vals @fragments) libraries)
                                       :sources uses-tags
                                       :document-type "OpenApi Fragment"})
        (assoc :raw (get node (keyword "@raw"))))))


(defmethod parse-ast :library [node context]
  (let [location (syntax/<-location node)
        _ (debug "Parsing OpenAPI Library at " location)
        fragments (or (:fragments context) (atom {}))
        annotations (atom {})
        uses-tags (process-uses-tags node context)
        ;; we parse traits and types and add the information into the context
        traits(domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                  :base-uri location
                                                                  :fragments fragments
                                                                  :annotations annotations
                                                                  :document-parser parse-ast})
        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :base-uri location
                                                                 :fragments fragments
                                                                 :annotations annotations
                                                                 :document-parser parse-ast})
        declarations (merge traits types)]
    (-> (document/map->ParsedModule (merge context
                                           {:id location
                                            :base-uri location
                                            :location location
                                            :declares (concat (vals declarations) (vals @annotations))
                                            :references (vals @fragments)
                                            :tags uses-tags
                                            :document-type "OpenAPI Library"}))
        (assoc :raw (get node (keyword "@raw"))))))
