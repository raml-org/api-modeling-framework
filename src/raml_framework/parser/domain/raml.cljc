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
   :description #{:root :resource}
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
   :securedBy #{:root :resource}
   :uses #{:root}
   :displayName #{:resource}
   :get #{:resource}
   :patch #{:resource}
   :put #{:resource}
   :post #{:resource}
   :delete #{:resource}
   :options #{:resource}
   :head #{:resource}
   :is #{:resource}
   :type #{:resource}
   :uriParameters #{:resource}})

(defn guess-type-from-predicates [x]
  (->> [(fn [x] (when (string/starts-with? (name x) "/") #{:root :resource}))]
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


(defn generate-parse-node-sources [location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/node-parsed")
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

(defn extract-nested-resources [node]
  (->> node
       (filter (fn [[k v]]
                 (string/starts-with? (str k) ":/")))
       (map (fn [[k v]]
              {:path (-> k str (string/replace-first ":/" "/"))
               :resource v}))))

(defn parse-nested-resources [extracted-resources location parsed-location context]
  (->> extracted-resources
       (map (fn [i {:keys [path resource]}]
              (let [context (-> context
                                (assoc :location (str location (utils/sanitize-path path)))
                                (assoc :parsed-location (str parsed-location "/resources/" i))
                                (assoc :path path))]
                (parse-ast resource context)))
            (range 0 (count extracted-resources)))))

(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment] :as context}]
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        nested-resources (-> node
                             (extract-nested-resources)
                             (parse-nested-resources location parsed-location context))
        properties {:id parsed-location
                    :sources (generate-parse-node-sources location parsed-location)
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
                    :license nil
                    :nested-endpoints nested-resources}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties properties})
      (domain/map->ParsedAPIDocumentation properties))))

(defmethod parse-ast :resource [node {:keys [location parsed-location is-fragment path] :as context}]
  (when (nil? path)
    (throw (new #? (:cljs js/Error :clj Exception)
                "Cannot parse a resource without information about the resource path in the context")))
  (let [nested-resources (-> node
                             (extract-nested-resources)
                             (parse-nested-resources location parsed-location context))
        properties {:path path
                    :sources (generate-parse-node-sources location parsed-location)
                    :id parsed-location
                    :name (:displayName node)
                    :description (:description node)
                    :supported-operations []
                    :nested-endpoints nested-resources}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-end-point
                                        :properties properties})
      (domain/map->ParsedEndPoint properties))))
