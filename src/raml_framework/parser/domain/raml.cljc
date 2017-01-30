(ns raml-framework.parser.domain.raml
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.set :as set]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [debug]]))

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

(defn parse-nested-resources [extracted-resources parent-path location parsed-location context]
  (->> extracted-resources
       (map (fn [i {:keys [path resource]}]
              (let [context (-> context
                                (assoc :parent-path (str parent-path path))
                                (assoc :location (str location (if (string/ends-with? location "/") "" "/")
                                                      (url/url-encode path)))
                                (assoc :parsed-location (str parsed-location "/end-points/" i))
                                (assoc :resource-path path)
                                (assoc :path path))]
                (parse-ast resource context)))
            (range 0 (count extracted-resources)))
       flatten))

(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing RAML root")
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        nested-resources (-> node
                             (utils/extract-nested-resources)
                             (parse-nested-resources "" location parsed-location context))
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
                    :endpoints nested-resources}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties properties})
      (domain/map->ParsedAPIDocumentation properties))))

(defn generate-resource-nesting-sources [path nested-ids location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/0/nested-resource-parsed")
        node-parsed-tag (document/->NestedResourceParsedTag source-map-id path)
        nested-children-tags (mapv (fn [i nested-id]
                                     (let [source-map-id (str parsed-location "/source-map/" (inc i) "/nested-children")]
                                       (document/->ResourceNestedChildrenTag source-map-id nested-id)))
                                   (range 0 (count nested-ids))
                                   nested-ids)]
    (debug "Generated " (count nested-children-tags) " child resource tags for resource " location)
    (flatten [(document/->DocumentSourceMap (str parsed-location "/source-map/0") location [node-parsed-tag])
              (mapv (fn [i child-tag]
                      (document/->DocumentSourceMap (str parsed-location "/source-map/" (inc i)) location [child-tag]))
                    (range 0 (count nested-children-tags))
                    nested-children-tags)])))

(defmethod parse-ast :resource [node {:keys [location parsed-location is-fragment resource-path parent-path] :as context}]
  (debug "Parsing resource " location)
  (let [extracted-resources (utils/extract-nested-resources node)
        extracted-paths-set (set (map :path extracted-resources))
        nested-resources (parse-nested-resources extracted-resources parent-path location parsed-location context)
        nested-children (->> nested-resources
                             (filter (fn [resource]
                                       (let [resource-path (first (document/find-tag resource document/nested-resource-parsed-tag))]
                                         (and (some? resource-path)
                                              (some? (extracted-paths-set (document/value resource-path))))))))
        nested-ids (if is-fragment
                     (mapv :id nested-children)
                     (mapv #(document/id %) nested-children))
        properties {:path parent-path
                    :sources (concat (generate-parse-node-sources location parsed-location)
                                     (generate-resource-nesting-sources resource-path nested-ids location parsed-location))
                    :id parsed-location
                    :name (:displayName node)
                    :description (:description node)
                    :supported-operations []}]
    (concat (if is-fragment
              [(domain/map->ParsedDomainElement {:id parsed-location
                                                 :fragment-node :parsed-end-point
                                                 :properties properties})]
              [(domain/map->ParsedEndPoint properties)])
            (or nested-resources []))))
