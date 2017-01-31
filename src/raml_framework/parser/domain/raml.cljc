(ns raml-framework.parser.domain.raml
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.set :as set]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(def properties-map
  {
   :annotationTypes #{:root}
   :baseUri #{:root}
   :baseUriParameters #{:root}
   :body #{:method}
   :delete #{:resource}
   :description #{:root :resource :method}
   :displayName #{:resource :method}
   :documentation #{:root}
   :get #{:resource}
   :head #{:resource}
   :headers #{:method}
   :is #{:resource :method}
   :mediaType #{:root}
   :options #{:resource}
   :patch #{:resource}
   :post #{:resource}
   :protocols? #{:root :method}
   :put #{:resource}
   :queryParameters #{:method}
   :queryString #{:method}
   :resourceTypes #{:root}
   :responses #{:method}
   :schemas #{:root}
   :securedBy #{:root :resource :method}
   :securitySchemes #{:root}
   :title #{:root}
   :traits #{:root}
   :type #{:resource}
   :types #{:root}
   :uriParameters #{:resource}
   :uses #{:root}
   :version #{:root}
   })

(defn guess-type-from-predicates [x]
  (->> [(fn [x] (when (string/starts-with? (str x) ":/") #{:root :resource}))]
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
  ;; if a type hint is available, we use that information to dispatch, otherwise we try to guess from properties
  (if (some? (:type-hint context))
    (:type-hint context)
    (guess-type node)))

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

(defn root->scheme [{:keys [protocols baseUri]}]
  (cond
    (some? protocols)                  (->> [protocols] flatten (map string/lower-case))
    (and (some? baseUri)
         (some? (:protocol
                 (url/url baseUri)))) [(:protocol (url/url baseUri))]
    :else                              nil))


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
                (parse-ast resource (assoc context :type-hint :resource))))
            (range 0 (count extracted-resources)))
       flatten))

(defn nested-resources-tags [is-fragment resource-id resource-path location parent-id nested-resources]
  "For each resource node we generate a source map with the path as it appears in the document and a additional children tag for every nested resource"
  (let [;; All descendants will be returned in nested-resources.
        ;; To be able to re-construct the tree we need only direct children. We try to find them looking for the id
        ;; they have in the nested-parent tag and checking if it matches the current resource
        nested-children (->> nested-resources
                             (filter (fn [resource]
                                       (let [resource-path (first (document/find-tag resource document/nested-resource-parent-id-tag))]
                                         (= resource-id (document/value resource-path))))))
        nested-ids (if is-fragment
                     (mapv :id nested-children)
                     (mapv #(document/id %) nested-children))
        source-map-id (str resource-id "/source-map/0/nested-resource-parsed")
        ;; the path tag
        node-parsed-tag (document/->NestedResourcePathParsedTag source-map-id resource-path)
        ;; the parent id tag
        node-parent-tag (document/->NestedResourceParentIdTag source-map-id parent-id)
        ;; the children resource tags
        nested-children-tags (mapv (fn [i nested-id]
                                     (let [source-map-id (str resource-id "/source-map/" (+ i 2) "/nested-children")]
                                       (document/->NestedResourceChildrenTag source-map-id nested-id)))
                                   (range 0 (count nested-ids))
                                   nested-ids)]
    (debug "Parsed " (count nested-children-tags) " child resource tags for resource " location)
    (flatten [(document/->DocumentSourceMap (str resource-id "/source-map/0") location [node-parsed-tag])
              (document/->DocumentSourceMap (str resource-id "/source-map/1") location [node-parent-tag])
              (mapv (fn [i child-tag]
                      (document/->DocumentSourceMap (str resource-id "/source-map/" (+ i 2)) location [child-tag]))
                    (range 0 (count nested-children-tags))
                    nested-children-tags)])))

(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing RAML root")
  (let [parsed-location (str parsed-location "/api-documentation")
        location (str location "/")
        ;; we generated all the descendants, flattening the hierarchy of nested resources
        ;; we keep the information about the tree structure in the tags of the parsed end-points
        nested-resources (-> node
                             (utils/extract-nested-resources)
                             (parse-nested-resources "" location parsed-location (assoc context :parent-id parsed-location)))
        children-tags (nested-resources-tags is-fragment
                                             location
                                             ""
                                             location
                                             parsed-location
                                             nested-resources)
        properties {:id parsed-location
                    :sources (concat (generate-parse-node-sources location parsed-location)
                                     children-tags)
                    :name (:title node)
                    :description (:description node)
                    :host (base-uri->host (:baseUri node))
                    :scheme (root->scheme node)
                    :base-path (base-uri->basepath (:baseUri node))
                    :accepts (filter some? (flatten [(:mediaType node)]))
                    :content-type (filter some? (flatten [(:mediaType node)]))
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

(defmethod parse-ast :resource [node {:keys [location parsed-location is-fragment resource-path parent-path parent-id] :as context}]
  (debug "Parsing resource " location)
  (let [resource-id parsed-location
        nested-resources (-> node
                             (utils/extract-nested-resources)
                             (parse-nested-resources parent-path location resource-id (assoc context :parent-id resource-id)))
        children-tags (nested-resources-tags is-fragment
                                             resource-id
                                             resource-path
                                             location
                                             parent-id
                                             nested-resources)
        operations (->> [:get :patch :put :post :delete :options :head]
                        (map (fn [op] (if-let [node (get node op)]
                                       (parse-ast node (-> context
                                                           (assoc :method (name op))
                                                           (assoc :type-hint :method)))
                                       nil)))
                        (filter some?))
        properties {:path parent-path
                    :sources (concat (generate-parse-node-sources location resource-id)
                                     children-tags)
                    :id resource-id
                    :name (:displayName node)
                    :description (:description node)
                    :supported-operations operations}]
    (concat (if is-fragment
              [(domain/map->ParsedDomainElement {:id resource-id
                                                 :fragment-node :parsed-end-point
                                                 :properties properties})]
              [(domain/map->ParsedEndPoint properties)])
            (or nested-resources []))))

(defmethod parse-ast :method [node {:keys [location parsed-location is-fragment method] :as context}]
  (debug "Parsing method " method)
  (let [method-id (str parsed-location "/" method)
        location (str location "/" method)
        node-parsed-source-map (generate-parse-node-sources location method-id)
        properties (-> {:id method-id
                        :sources node-parsed-source-map
                        :method method
                        :name (:displayName node)
                        :description (:description node)
                        :scheme (:protocols node)
                        :headers (:headers node)}
                       utils/clean-nils)]
    (if is-fragment
      (domain/map->ParsedOperation {:id method-id
                                    :fragment :parsed-operation
                                    :properties properties})
      (domain/map->ParsedOperation properties))))
