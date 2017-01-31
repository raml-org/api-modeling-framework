(ns raml-framework.parser.domain.openapi
  (:require [clojure.string :as string]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [clojure.set :as set]
            [cemerick.url :as url]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))


(def properties-map
  {
   :basePath #{:swagger}
   :consumes #{:swagger :operation}
   :contact #{:info}
   :definitions #{:swagger}
   :delete #{:path-item}
   :deprecated #{:operation}
   :description #{:info :operation}
   :externalDocs #{:swagger :operation}
   :get #{:path-item}
   :head #{:path-item}
   :host #{:swagger}
   :info #{:swagger}
   :license #{:info}
   :operationId #{:operation}
   :options #{:path-item}
   :parameters #{:swagger :path-item :operation}
   :patch #{:path-item}
   :paths #{:swagger}
   :post #{:path-item}
   :produces #{:swagger :operation}
   :put #{:path-item}
   :responses #{:swagger :operation}
   :schemes #{:swagger :operation}
   :security #{:swagger :operation}
   :securityDefinitions #{:swagger}
   :summary #{:operation}
   :swagger #{:swagger}
   :tags #{:swagger :operation}
   :termsOfService #{:info}
   :title #{:info}
   :version #{:info}
   })

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

(defn parse-ast-dispatch-function [node context]
  (if (some? (:type-hint context))
    (:type-hint context)
    (guess-type node)))

(defmulti parse-ast (fn [node context] (parse-ast-dispatch-function node context)))

(defn generate-parsed-node-sources [node-name location parsed-location]
  (let [source-map-parsed-location (str parsed-location "/source-map/" node-name)
        node-parsed-tag (document/->NodeParsedTag source-map-parsed-location location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defn generate-open-api-tags-sources [tags location parsed-location]
  (let [tags (or tags [])]
    (->> tags
         (map (fn [i tag]
                (let [parsed-location (str parsed-location "/source-map/api-tags-" i "/tag")]
                  (document/->DocumentSourceMap
                   (str parsed-location "/source-map/api-tags-" i)
                   location
                   [(document/->APITagTag parsed-location tag)])))
              (range 0 (count tags))))))

(defmethod parse-ast :swagger [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing swagger")
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        sources (generate-parsed-node-sources "root" location parsed-location)
        endpoints (parse-ast (:paths node) (-> context
                                               (assoc :type-hint :paths)
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
                                                                              (assoc :type-hint :info)
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
                                                     :sources (generate-parsed-node-sources "info" location parsed-location)
                                                     :terms-of-service (:termsOfService node)}}))))

(defmethod parse-ast :paths [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing paths")
  (let [location (str location "paths")
        paths-sources (generate-parsed-node-sources "paths" location parsed-location)
        nested-resources (utils/extract-nested-resources node)]
    (map (fn [i {:keys [path resource]}]
           (let [context (-> context
                             (assoc :type-hint :path-item)
                             (assoc :location (str location "/" (url/url-encode path)))
                             (assoc :parsed-location (str parsed-location "/end-points/" i))
                             (assoc :path path)
                             (assoc :paths-sources paths-sources))]
             (parse-ast resource context)))
         (range 0 (count nested-resources))
         nested-resources)))

(defmethod parse-ast :path-item [node {:keys [location parsed-location is-fragment path paths-sources] :as context}]
  (debug "Parsing path-item")
  (when (nil? path)
    (throw (new #?(:clj Exception :cljs js/Error) "Cannot parse path-item object without contextual path information")))
  (let [operations (->> [:get :put :post :delete :options :head :patch]
                        (map (fn [op] (if-let [method-node (get node op)]
                                       (parse-ast method-node (-> context
                                                                  (assoc :type-hint :operation)
                                                                  (assoc :method (name op))))
                                       nil)))
                        (filter some?))
        properties {:path path
                    :sources (concat (generate-parsed-node-sources "path-item" location parsed-location) (or paths-sources []))
                    :id parsed-location
                    :supported-operations operations}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-end-point
                                        :properties properties})
      (domain/map->ParsedEndPoint properties))))

(defmethod parse-ast :operation [node {:keys [location parsed-location is-fragment method] :as context}]
  (debug "Parsing method " method)
  (let [method-id (str parsed-location "/" method)
        location (str location "/" method)
        node-parsed-source-map (generate-parsed-node-sources method location parsed-location)
        api-tags (generate-open-api-tags-sources (:tags node) location parsed-location)
        properties {:id method-id
                    :method method
                    :sources (concat node-parsed-source-map api-tags)
                    :name (:operationId node)
                    :description (:description node)
                    :scheme (:schemes node)
                    :accepts (:consumes node)
                    :content-type (:produces node)}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-operation
                                        :properties properties})
      (domain/map->ParsedOperation properties))))

(defmethod parse-ast :undefined [_ _]
  (debug "Parsing undefined")
  nil)
