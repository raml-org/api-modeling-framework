(ns raml-framework.parser.domain.openapi
  (:require [clojure.string :as string]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [clojure.set :as set]
            [cemerick.url :as url]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [debug]]))


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
   :parameters #{:swagger :path-info}
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
   :version #{:info}
   :get #{:path-info}
   :put #{:path-info}
   :post #{:path-info}
   :delete #{:path-info}
   :options #{:path-info}
   :head #{:path-info}
   :patch #{:path-info}})

(defn guess-type-from-predicates [x]
  (->> [(fn [x] (when (string/starts-with? (str x) ":/") #{:paths}))]
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

(defn generate-parsed-node-sources [node-name location parsed-location]
  (let [source-map-parsed-location (str parsed-location "/source-map/" node-name)
        node-parsed-tag (document/->NodeParsedTag source-map-parsed-location location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defmethod parse-ast :swagger [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing swagger")
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        sources (generate-parsed-node-sources "node-parsed-root" location parsed-location)
        endpoints (parse-ast (:paths node) (-> context
                                               (assoc :parsed-location parsed-location)
                                               (assoc :location location)))
        properties {:id parsed-location
                    :host (:host node)
                    :scheme (flatten [(:schemes node)])
                    :base-path (:basePath node)
                    :accepts (flatten [(:consumes node)])
                    :content-type (flatten [(:produces node)])
                    :provider nil
                    :license nil
                    :endpoints endpoints}
        node-info-properties (if (some? (:info node))
                               (domain/properties (parse-ast (:info node) (-> context
                                                                              (assoc :is-fragment true)
                                                                              (assoc :parsed-location parsed-location)
                                                                              (assoc :location location))))
                               {})
        sources (concat (get node-info-properties :sources []) sources)
        properties (merge properties (dissoc node-info-properties :fragment-node))
        properties (assoc properties :sources sources)]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties {:id parsed-location
                                                     :fragment-node :parsed-api-documentation
                                                     :properties properties}})
      (domain/map->ParsedAPIDocumentation properties))))

(defmethod parse-ast :info [node {:keys [location parsed-location is-fragment]}]
  (debug "Parsing info")
  (let [location (str location "info")]
    (if (not is-fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot parsed domain element from info node at " location)))
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties {:name (:title node)
                                                     :fragment-node :info
                                                     :description (:description node)
                                                     :version (:version node)
                                                     :sources (generate-parsed-node-sources "node-parsed-info" location parsed-location)
                                                     :terms-of-service (:termsOfService node)}}))))

(defmethod parse-ast :paths [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing paths")
  (let [location (str location "paths")
        paths-sources (generate-parsed-node-sources "paths-node" location parsed-location)
        nested-resources (utils/extract-nested-resources node)]
    (map (fn [i {:keys [path resource]}]
           (let [context (-> context
                             (assoc :location (str location "/" (url/url-encode path)))
                             (assoc :parsed-location (str parsed-location "/end-points/" i))
                             (assoc :path path)
                             (assoc :paths-sources paths-sources))]
             (parse-ast resource context)))
         (range 0 (count nested-resources))
         nested-resources)))

(defmethod parse-ast :path-info [node {:keys [location parsed-location is-fragment path paths-sources] :as context}]
  (debug "Parsing path-info")
  (when (nil? path)
    (throw (new #?(:clj Exception :cljs js/Error) "Cannot parse path-info object without contextual path information")))
  (let [properties {:path path
                    :sources (concat (generate-parsed-node-sources "path-info-node" location parsed-location) (or paths-sources []))
                    :id parsed-location
                    :supported-operations []}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-end-point
                                        :properties properties})
      (domain/map->ParsedEndPoint properties))))

(defmethod parse-ast :undefined [_ _]
  (debug "Parsing undefined")
  nil)
