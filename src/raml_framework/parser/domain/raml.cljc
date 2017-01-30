(ns raml-framework.parser.domain.raml
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.set :as set]))

(def properties-map
  {:title #{:root}
   :description #{:root}
   :version #{:root}
   :baseUri #{:root}
   :baseUriParameters #{:root}
   :protocols? #{:root}
   :mediaType #{:root}
   :documentation #{:root}
   :schemas #{:root}
   :types #{:root}
   :traits #{:root}
   :annotationTypes #{:root}
   :resourceTypes #{:root}
   :securitySchemes #{:root}
   :securedBy #{:root}
   :uses #{:root}})

(defn guess-type-from-predicates [x]
  (->> [(fn [x] (when (string/starts-with? (name x) "/") #{:root}))]
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
  (let [source-map-id (str parsed-location "/source-map/node-parsed-root")
        node-parsed-tag (document/->NodeParsedTag source-map-id location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defn base-uri->host [base-uri]
  (when (some? base-uri)
    (let [{:keys [host]} (url/url base-uri)]
      host)))

(defn base-uri->basepath [base-uri]
  (when (some? base-uri)
    (let [{:keys [path]} (url/url base-uri)]
      path)))

(defn root->scheme [{:keys [protocols]}]
  (if (some? protocols)
    (->> [protocols] flatten (map string/lower-case))
    nil))


(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment]}]
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        properties {:id parsed-location
                    :sources (generate-root-node-sources location parsed-location)
                    :name (:title node)
                    :description (:description node)
                    :host (base-uri->host (:baseUri node))
                    :scheme (root->scheme node)
                    :base-path (base-uri->basepath (:baseUri node))
                    :accepts (flatten [(:mediaType node)])
                    :content-type (flatten [(:mediaType node)])
                    :version (:version node)
                    :provider nil
                    :terms-of-service nil
                    :license nil}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties properties})
      (domain/map->ParsedAPIDocumentation properties))))
