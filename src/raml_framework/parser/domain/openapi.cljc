(ns raml-framework.parser.domain.openapi
  (:require [clojure.string :as string]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [clojure.set :as set]))


(def properties-map
  {:swagger #{:swagger}
   :info #{:swagger}
   :host #{:swagger}
   :basePath #{:swagger}
   :schemes #{:swagger}
   :consumes #{:swagger}
   :produces #{:swagger}
   :paths #{:swagger}
   :definitions #{:swagger}
   :parameters #{:swagger}
   :responses #{:swagger}
   :securityDefinitions #{:swagger}
   :security #{:swagger}
   :tags #{:swagger}
   :externalDocs #{:swagger}
   :title #{:info}
   :description #{:info}
   :termsOfService #{:info}
   :contact #{:info}
   :license #{:info}
   :version #{:info}})

(defn guess-type-from-predicates [x]
  (->> [] ;; no predicates yet
       (map (fn [p] (p x)))
       (filter some?)
       first))

(defn guess-type [node]
  (let [node-types (->> node
                        (map (fn [[k _]] (get properties-map k (guess-type-from-predicates k))))
                        (filter some?))
        node-type (first (if (empty? node-types) [] (apply set/intersection node-types)))]
    (or node-type :undefined)))

(defn parse-ast-dispatch-function [node context] (guess-type node))

(defmulti parse-ast (fn [node context] (parse-ast-dispatch-function node context)))

(defn generate-root-node-sources [location parsed-location]
  (let [source-map-parsed-location (str parsed-location "/source-map/node-parsed-root")
        node-parsed-tag (document/->NodeParsedTag source-map-parsed-location location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defmethod parse-ast :swagger [node {:keys [location parsed-location is-fragment] :as context}]
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        sources (generate-root-node-sources location parsed-location)
        properties {:id parsed-location
                    :host (:host node)
                    :scheme (flatten [(:schemes node)])
                    :base-path (:basePath node)
                    :accepts (flatten [(:consumes node)])
                    :content-type (flatten [(:produces node)])
                    :provider nil
                    :license nil}
        node-info-properties (if (some? (:info node))
                               (domain/properties (parse-ast (:info node) (-> context
                                                                              (assoc :is-fragment true)
                                                                              (assoc :parsed-location parsed-location)
                                                                              (assoc :location location))))
                               {})
        sources (concat (get node-info-properties :sources []) sources)
        properties (merge properties node-info-properties)
        properties (assoc properties :sources sources)]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties {:id parsed-location
                                                     :fragment-node :parsed-api-documentation
                                                     :properties properties}})
      (domain/map->ParsedAPIDocumentation properties))))

(defn generate-info-node-sources [location parsed-location]
  (let [source-map-parsed-location (str parsed-location "/source-map/node-parsed-info")
        node-parsed-tag (document/->NodeParsedTag source-map-parsed-location location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defmethod parse-ast :info [node {:keys [location  parsed-location is-fragment]}]
  (let [location (str location "/info")]
    (if (not is-fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot parsed domain element from info node at " location)))
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties {:name (:title node)
                                                     :fragment-node :info
                                                     :description (:description node)
                                                     :version (:version node)
                                                     :sources (generate-info-node-sources location parsed-location)
                                                     :terms-of-service (:termsOfService node)}}))))
