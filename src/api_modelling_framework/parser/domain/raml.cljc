(ns api-modelling-framework.parser.domain.raml
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.common :as common]
            [api-modelling-framework.parser.domain.json-schema-shapes :as json-schema-shapes]
            [api-modelling-framework.parser.domain.raml-types-shapes :as shapes]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.platform :as platform]
            [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.set :as set]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(def raml-types-properties-map
  {
   "any" #{:type}
   "object" #{:type}
   "array" #{:type}
   "union" #{:type}
   "time-only" #{:type}
   "datetime" #{:type}
   "datetime-only" #{:type}
   "date-only" #{:type}
   "number" #{:type}
   "boolean" #{:type}
   "string" #{:type}
   "null" #{:type}
   "file" #{:type}
   "integer" #{:type}
   })

(def properties-map
  {
   :annotationTypes #{:root}
   :baseUri #{:root}
   :baseUriParameters #{:root}
   :body #{:method :response}
   :delete #{:resource}
   :description #{:root :resource :method :response :type}
   :displayName #{:resource :method :type}
   :documentation #{:root}
   :get #{:resource}
   :head #{:resource}
   :headers #{:method :response}
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
   :type #{:resource :type}
   :properties #{:type}
   :types #{:root}
   :uriParameters #{:resource}
   :uses #{:root :method :resource :type}
   :version #{:root}
   })



(defn  extract-scalar
  "Function used to unwrap an scalar value if it defined using the value syntax"
  [x]
  (cond
    (and (map? x) (:value x)) (:value x)
    (coll? x)                 (mapv extract-scalar x)
    :else                     x))

(defn guess-type-from-predicates
  "If the AST node is a string it can be one of these nodes: resource path, responses tatus or a media type, this function check for these occurrences.
   Used in the dispatcher function"
  [x]
  (->> [(fn [x] (when (string/starts-with? (utils/safe-str x) "/") #{:root :resource}))
        (fn [x] (when (some? (re-matches #"^\d+$" (utils/safe-str x))) #{:responses}))
        (fn [x] (when (some? (re-matches #"^[a-z]+/[a-z+]+$" (utils/safe-str x))) #{:body-media-type}))]
       (mapv (fn [p] (p x)))
       (filterv some?)
       first))

(defn guess-type
  "Based in the keys of the node map, we assign a type to the node, used in the dispatcher function."
  [node]
  (let [node-types (->> node
                        (mapv (fn [[k _]]
                                (get properties-map k (guess-type-from-predicates k))))
                        (filterv some?))
        node-type (first (if (empty? node-types) [] (apply set/intersection node-types)))]
    (or node-type :undefined)))

(defn check-reference
  "Checks if a provided string points to one of the types defined at the APIDocumentation level"
  [node-str {:keys [references]}]
  (if (or (some? (get references (keyword (utils/safe-str node-str))))
          (some? (get raml-types-properties-map (utils/safe-str node-str))))
    :type
    :undefined))

(defn parse-ast-dispatch-function
  "If a type hint is available, we use that information to dispatch, otherwise we try to guess from properties"
  [node context]
  (cond
    (some? (syntax/<-location node)) :fragment
    (some? (:type-hint context))     (:type-hint context)
    (string? node)                   (check-reference node context)
    :else                            (guess-type node)))

(defmulti parse-ast (fn [node context] (parse-ast-dispatch-function node context)))

(defn annotation? [x] (and (string/starts-with? (utils/safe-str x) "(")
                           (string/ends-with? (utils/safe-str x) ")")))

(defn parse-annotation-ast [p model {:keys [annotations location]}]
  (let [annotation-name (-> p
                            utils/safe-str
                            (string/replace #"\(" "")
                            (string/replace #"\)" ""))
        annotation (get annotations annotation-name)]
    (if (nil? annotation)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find annotation " p)))
      (do
        (->> (domain/map->ParsedDomainProperty {:id (document/id annotation)
                                                :name annotation-name
                                                :object (utils/annotation->jsonld model)})
             (common/with-location-meta-from model))))))

(defn with-annotations [node ctx model]
  (if (map? node)
    (let [parsed-annotations (->> node
                                  (filter (fn [[k v]] (annotation? k)))
                                  (map (fn [[k v]] (parse-annotation-ast k v ctx))))]
      (if (> (count parsed-annotations) 0)
        (if (some? (:properties model))
          (assoc-in model [:properties :additional-properties] parsed-annotations)
          (assoc model :additional-properties parsed-annotations))
        model))
    model))

(defn generate-parse-node-sources
  "Source map pointing to the incoming node for this AST node"
  [location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/node-parsed")
        node-parsed-tag (document/->NodeParsedTag source-map-id location)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [node-parsed-tag] [])]))

(defn parse-parameters [type location-segment parameters {:keys [location parsed-location is-fragment] :as context}]
  ;; parameters are 'properties' nodes, that's the reason they can be required, etc.
  (if (nil? parameters) []
      (->> parameters
           (mapv (fn [[property-name property-value]]

                   (let [property-value (if (string? property-value) {:type property-value} property-value)
                         property-type (get property-value :type "string")
                         property-value (assoc property-value :type property-type)
                         parsed-location (utils/path-join location location-segment (url/url-encode (utils/safe-str property-name)))
                         location (utils/path-join location  location-segment (url/url-encode (utils/safe-str property-name)))
                         node-parsed-source-map (generate-parse-node-sources location parsed-location)
                         required (shapes/required-property? property-name property-value)
                         property-name (shapes/final-property-name property-name property-value)
                         property-shape (shapes/parse-type property-value (-> context
                                                                          (assoc :location location)
                                                                          (assoc :parsed-location parsed-location)
                                                                          (assoc :parse-ast parse-ast)))
                         properties {:id parsed-location
                                     :name (utils/safe-str property-name)
                                     :sources node-parsed-source-map
                                     :required required
                                     :parameter-kind type
                                     :shape property-shape}]
                     (with-annotations property-value context
                       (->> (domain/map->ParsedParameter properties)
                            (common/with-location-meta-from property-value)))))))))

(defn base-uri->host [base-uri]
  (when (some? base-uri)
    (let [base-uri (if (string/index-of base-uri "://") base-uri (str "http://" base-uri))
          {:keys [host]} (url/url base-uri)]
      host)))

(defn base-uri->basepath [base-uri]
  (when (some? base-uri)
    (let [base-uri (if (string/index-of base-uri "://") base-uri (str "http://" base-uri))
          {:keys [path]} (url/url base-uri)]
      path)))

(defn root->scheme [{:keys [protocols baseUri]}]
  (let [baseUri (extract-scalar baseUri)]
    (cond
      (some? protocols)                  (->> [protocols] flatten (mapv string/lower-case))
      (and (some? baseUri)
           (string/index-of baseUri "://")
           (some? (:protocol
                   (url/url baseUri)))) [(:protocol (url/url baseUri))]
      :else                              nil)))


(defn parse-nested-resources [extracted-resources parent-path location parsed-location context]
  (->> extracted-resources ;; this comes from utils/extract-nested-resources it returns a map with {:path :resource} keys
       (mapv (fn [i {:keys [path resource]}]
               (let [context (-> context
                                 (assoc :parent-path (str parent-path path))
                                 (assoc :location (utils/path-join location (url/url-encode path)))
                                 (assoc :parsed-location (utils/path-join parsed-location "end-points" i))
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
                             (filterv (fn [resource]
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
    (flatten [(document/->DocumentSourceMap (utils/path-join resource-id "/source-map/0") location [node-parsed-tag] [])
              (document/->DocumentSourceMap (utils/path-join resource-id "/source-map/1") location [node-parent-tag] [])
              (mapv (fn [i child-tag]
                      (document/->DocumentSourceMap (str resource-id "/source-map/" (+ i 2)) location [child-tag] []))
                    (range 0 (count nested-children-tags))
                    nested-children-tags)])))

(defn generate-inline-fragment-parsed-sources [parsed-location fragment-name fragment-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/inline-fragment/" fragment-name)
        inline-fragment-parsed-tag (document/->InlineFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map")
                                   fragment-location
                                   [inline-fragment-parsed-tag]
                                   [])]))

(defn generate-extend-include-fragment-sources [parsed-location fragment-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/inline-fragment")
        parsed-tag (document/->ExtendIncludeFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map")
                                   fragment-location
                                   [parsed-tag]
                                   [])]))

(defn generate-is-trait-sources [trait-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/is-trait")
        is-trait-tag (document/->IsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [is-trait-tag] [])]))



(defn process-annotations [node {:keys [base-uri location parsed-location] :as context}]
  (debug "Processing " (count (:annotationTypes node {})) " annotations")
  (let [location (utils/path-join location "/x-annotationTypes")
        nested-context (-> context (assoc :location location) (assoc :parsed-location (str base-uri "#")))]
    (->> (:annotationTypes node {})
         (reduce (fn [acc [annotation-name annotation-node]]
                   (let [encoded-annotation-name (url/url-encode (utils/safe-str annotation-name))
                         range (parse-ast annotation-node (-> nested-context
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

(defn process-traits [node {:keys [location parsed-location] :as context}]
  (debug "Processing " (count (:traits node [])) "traits")
  (let [location (utils/path-join location "/traits")
        ;; this must be 'x-traits' so the generated path matches the position
        ;; in JSON-LD documents
        parsed-location (utils/path-join parsed-location "/x-traits")
        nested-context (-> context (assoc :location location) (assoc :parsed-location parsed-location))]
    (->> (:traits node {})
         (reduce (fn [acc [trait-name trait-node]]
                   (debug (str "Processing trait " trait-name))
                   (let [fragment-name (url/url-encode (utils/safe-str trait-name))
                         trait-fragment (parse-ast trait-node (-> nested-context
                                                                  (assoc :location location)
                                                                  (assoc :parsed-location (utils/path-join parsed-location fragment-name))
                                                                  (assoc :type-hint :method)))
                         trait-fragment (-> trait-fragment
                                            (assoc :method nil) ;; method must be nil, this information cannot be in the fragment
                                            (assoc :id (utils/path-join parsed-location fragment-name))
                                            (assoc :abstract true)
                                            (assoc :name fragment-name))
                         sources (or (:sources trait-fragment) [])
                         sources (concat sources (generate-is-trait-sources fragment-name
                                                                            (utils/path-join location fragment-name)
                                                                            (utils/path-join parsed-location fragment-name)))
                         parsed-trait (assoc trait-fragment :sources sources)]
                     (assoc acc (keyword (utils/alias-chain trait-name context)) parsed-trait)))
                 {}))))

(defn process-types [node {:keys [location parsed-location alias-chain] :as context}]
  (let [types (or (:types node) (:schemas node) {})
        path-label (if (some? (:types node)) "types" "schemas")
        location (utils/path-join parsed-location "/" path-label)
        nested-context (-> context (assoc :location location) (assoc :parsed-location parsed-location))]
    (debug "Processing " (count types) " types")
    (->> types
         (reduce (fn [acc [type-name type-node]]
                   (debug (str "Processing type " type-name))
                   (let [type-node (if (syntax/fragment? type-node)
                                     ;; avoiding situations where we transform this into an include
                                     ;; and then we cannot transform this back into type because there's
                                     ;; no way to tell it without source maps
                                     {:type type-node}
                                     type-node)
                         type-name  (url/url-encode (utils/safe-str type-name))
                         type-id (common/type-reference parsed-location type-name)
                         references (get nested-context :references {})
                         type-fragment (parse-ast type-node (-> nested-context
                                                                (assoc :references (merge references acc))
                                                                (assoc :location location)
                                                                (assoc :parsed-location type-id)
                                                                (assoc :is-fragment false)
                                                                (assoc :type-hint :type)))
                         sources (or (-> type-fragment :sources) [])
                         ;; we annotate the parsed type with the is-type source map so we can distinguish it from other declarations
                         sources (concat sources (common/generate-is-type-sources type-name
                                                                                  (utils/path-join location type-name)
                                                                                  type-id))
                         parsed-type (assoc type-fragment :sources sources)
                         parsed-type (if (nil? (:name parsed-type))
                                       (assoc parsed-type :name type-name)
                                       parsed-type)]
                     (assoc acc (keyword (utils/alias-chain type-name context)) parsed-type)))
                 {}))))

(defn find-extend-tags [{:keys [location parsed-location references] :as context}]
  (->> references
       (mapv (fn [[extend-name parsed-domain-element]]
               (condp = (:fragment-node parsed-domain-element)
                 :trait (generate-inline-fragment-parsed-sources (utils/path-join parsed-location "/traits")
                                                                 (name extend-name)
                                                                 (:id parsed-domain-element))
                 (generate-inline-fragment-parsed-sources (utils/path-join parsed-location "/declares")
                                                          (name extend-name)
                                                          (:id parsed-domain-element)))))
       flatten))

(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment references] :as context :or {references {}}}]
  (debug "Parsing RAML root")
  (let [parsed-location (utils/path-join parsed-location "/api-documentation")
        location (str location "/")
        ;; we generated all the descendants, flattening the hierarchy of nested resources
        ;; we keep the information about the tree structure in the tags of the parsed end-points
        nested-resources (-> node
                             (utils/extract-nested-resources)
                             (parse-nested-resources "" location parsed-location (assoc context :parent-id parsed-location)))
        trait-tags (find-extend-tags (-> context
                                         (assoc :location location)
                                         (assoc :parsed-location parsed-location)))
        children-tags (nested-resources-tags is-fragment
                                             location
                                             ""
                                             location
                                             parsed-location
                                             nested-resources)
        base-uri-parameters (parse-parameters "domain" "base-uri" (:baseUriParameters node {}) context)
        properties {:id parsed-location
                    :sources (concat (generate-parse-node-sources location parsed-location)
                                     children-tags
                                     trait-tags
                                     ;;types-sources
                                     )
                    :name (extract-scalar (:title node))
                    :parameters base-uri-parameters
                    :description (extract-scalar (:description node))
                    :host (utils/ensure-not-blank (base-uri->host (extract-scalar (:baseUri node))))
                    :scheme (utils/ensure-not-blank (root->scheme node))
                    :base-path (utils/ensure-not-blank (base-uri->basepath (extract-scalar (:baseUri node))))
                    :accepts (filterv some? (flatten [(extract-scalar (:mediaType node))]))
                    :content-type (filterv some? (flatten [(:mediaType node)]))
                    :version (extract-scalar (:version node))
                    :provider nil
                    :terms-of-service nil
                    :license nil
                    :endpoints nested-resources}]
    (->> (domain/map->ParsedAPIDocumentation properties)
         (with-annotations node context)
         (common/with-location-meta-from node))))

(defn generate-extends-trait-sources [trait-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/extend-trait")
        extends-trait-tag (document/->ExtendsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [extends-trait-tag] [])]))

(defn parse-traits [resource-id node references {:keys [location parsed-location] :as context}]
  (let [traits (flatten [(:is node [])])]
    (->> traits
         (mapv (fn [trait-name]
                 [trait-name (-> references
                                 (get (keyword trait-name)))]))
         (mapv (fn [i [trait-name trait]]
                 (if (some? trait)
                   (let
                       [extend-id (utils/path-join parsed-location "/extends/" (url/url-encode trait-name))
                        extend-location (utils/path-join location "/is/" i)
                        node-parsed-source-map (generate-parse-node-sources extend-location extend-id)
                        extends-trait-source-map (generate-extends-trait-sources trait-name extend-location extend-id)]
                     (->>
                       (document/map->ParsedExtends {:id extend-id
                                                     :sources (concat node-parsed-source-map
                                                                      extends-trait-source-map)
                                                     :target (document/id trait)
                                                     :label "trait"
                                                     :arguments []})
                       (with-annotations trait context)
                       (common/with-location-meta-from trait)))
                   (throw (new #?(:clj Exception :cljs js/Error)
                               (str "Cannot find trait '" trait-name "' to extend in node '" resource-id "'")))))
               (range 0 (count traits))))))

;; parent-path points to this resource concatenated path
;; resource-path is the value in the RAML spec that can be only the last segment of the path
(defmethod parse-ast :resource [node {:keys [location parsed-location is-fragment resource-path parent-path parent-id references] :as context}]
  (debug "Parsing resource " location)
  (let [resource-id parsed-location
        traits (parse-traits resource-id node references (assoc context :parsed-location resource-id))
        nested-resources (-> node
                             (utils/extract-nested-resources)
                             (parse-nested-resources parent-path location resource-id (assoc context :parent-id resource-id)))
        children-tags (nested-resources-tags is-fragment
                                             resource-id
                                             resource-path
                                             location
                                             parent-id
                                             nested-resources)
        uri-parameters (parse-parameters "path" "pathParameters"
                                         ;; path parameters are mandatory
                                         (:uriParameters node {})
                                         context)
        operations (->> node
                        keys
                        (mapv (fn [op] (if-let [operation (#{:set :get :post :patch :put :delete :head :options} op)]
                                        (assoc (parse-ast (get node operation {})
                                                          (-> context
                                                              (assoc :method (utils/safe-str op))
                                                              (assoc :parsed-location resource-id)
                                                              (assoc :type-hint :method)))
                                               :method (utils/safe-str operation))
                                        nil)))
                        (filterv some?))
        properties (utils/clean-nils {:path parent-path
                                      :sources (concat (generate-parse-node-sources location resource-id)
                                                       children-tags)
                                      :id resource-id
                                      :name (extract-scalar (:displayName node))
                                      :description (extract-scalar (:description node))
                                      :supported-operations operations
                                      :extends traits
                                      :parameters uri-parameters})]
    (concat [(->>
              (domain/map->ParsedEndPoint properties)
              (with-annotations node context)
              (common/with-location-meta-from node))]
            (or nested-resources []))))

(defn extract-bodies [node {:keys [location parsed-location] :as context}]
  (let [location (utils/path-join location "/body")
        parsed-location (utils/path-join parsed-location "/body")
        responses (flatten [(parse-ast node (-> context
                                                (assoc :location location)
                                                (assoc :parsed-operation parsed-location)
                                                (dissoc :type-hint)))])]
    (->> responses
         (mapv (fn [res]
                 (cond
                   (and (satisfies? domain/Type res)
                        (satisfies? document/Node res)) {:media-type nil
                                                         :location location
                                                         :body-id parsed-location
                                                         :schema res}
                   (some? (:media-type res))            res
                   (nil? res)                           nil
                   :else                                (throw (new #?(:clj Exception :cljs js/Error)
                                                                    (str "Cannot parse body response at " location ", media-type or type declaration expected")))))))))

(defn parse-http-payloads [body-id node {:keys [location parsed-location is-fragment] :as context}]
  (let [bodies (extract-bodies (:body node) context)]
    (->> bodies
         (filter some?)
         (mapv (fn [{:keys [media-type body-id location schema] :as data}]
                 (let [node-parsed-source-map (generate-parse-node-sources location body-id)]
                   (->>
                    (domain/map->ParsedPayload (utils/clean-nils {:id body-id
                                                                  :media-type (-> media-type
                                                                                  utils/safe-str
                                                                                  utils/ensure-not-blank)
                                                                  :schema schema
                                                                  :sources node-parsed-source-map}))
                     (with-annotations node context)
                     (common/with-location-meta-from node))))))))

(defn parse-request [node {:keys [location parsed-location] :as context}]
  (let [request-id (str parsed-location "/request")
        node-parsed-source-map (generate-parse-node-sources location request-id)
        query-parameters (parse-parameters "query" "queryParameters" (:queryParameters node) context)
        ;; @todo we need to fix the problem with the query-string
        ;;query-string (parse-parameters "query" "queryString" (:queryString node) next-context)
        query-string []
        headers (parse-parameters "header" "headers" (:headers node) context)
        payloads (parse-http-payloads (utils/path-join request-id "payload") node context)]
    (if (empty? (concat headers payloads query-string query-parameters))
      ;; nothing to generated
      nil
      ;; we have something to generate
      (->>
       (domain/map->ParsedRequest {:id request-id
                                   :sources node-parsed-source-map
                                   :parameters (concat query-parameters query-string)
                                   :headers headers
                                   :payloads payloads})
       (with-annotations node context)
       (common/with-location-meta-from node)))))

(defmethod parse-ast :method [node {:keys [location parsed-location is-fragment method references] :as context}]
  (debug "Parsing method " method)
  (let [method-id (utils/path-join parsed-location method)
        location (utils/path-join location method)
        next-context (-> context
                         (assoc :is-fragment false)
                         (assoc :parsed-location method-id)
                         (assoc :location location))
        node-parsed-source-map (generate-parse-node-sources location method-id)
        traits (parse-traits method-id node references next-context)
        request (parse-request node next-context)
        responses (parse-ast (:responses node {}) (assoc next-context :type-hint :responses))
        properties (-> {:id method-id
                        :sources node-parsed-source-map
                        :method (utils/safe-str method)
                        :name (extract-scalar (:displayName node))
                        :description (extract-scalar (:description node))
                        :scheme (extract-scalar (:protocols node))
                        :request request
                        :responses (flatten responses)
                        :extends traits}
                       utils/clean-nils)]
    (->>
     (domain/map->ParsedOperation properties)
     (with-annotations node context)
     (common/with-location-meta-from node))))

(defmethod parse-ast :responses [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing responses")
  (->> node
       (mapv (fn [[key response]]
               (parse-ast response (-> context
                                       (assoc :location (utils/path-join location "/responses"))
                                       (assoc :type-hint :response)
                                       (assoc :status-code key)))))))

(defmethod parse-ast :response [node {:keys [location parsed-location is-fragment status-code] :as context}]
  (debug "Parsing response " status-code)
  (let [response-id (utils/path-join parsed-location (utils/safe-str status-code))
        sources (generate-parse-node-sources location response-id)
        location (utils/path-join location status-code)
        next-context (-> context
                         (assoc :location location)
                         (assoc :parsed-location response-id))
        status-code (if (integer? status-code) (str status-code) (utils/safe-str status-code))
        description (extract-scalar (:description node))
        payloads (parse-http-payloads response-id node next-context)
        properties {:id response-id
                    :description description
                    :sources sources
                    :status-code status-code
                    :name (utils/safe-str status-code)
                    :payloads payloads}]
    (->>
     (domain/map->ParsedResponse properties)
     (with-annotations node context)
     (common/with-location-meta-from node))))

(defmethod parse-ast :body-media-type [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing body media-type")
  (->> node
       (mapv (fn [[media-type body]]
               (let [location (utils/path-join location (url/url-encode (utils/safe-str media-type)))
                     parsed-location (utils/path-join parsed-location (url/url-encode (utils/safe-str media-type)))]
                 (cond
                   ;; something like
                   ;; body:
                   ;;  application/xml
                   (or (nil? body)
                       (= "" body)
                       (= {} body))  {:media-type (-> media-type
                                                      utils/safe-str
                                                      utils/ensure-not-blank)
                                      :body-id parsed-location
                                      :location location}

                   ;; default body with a raml type for the media type
                   :else             {:media-type (-> media-type
                                                      utils/safe-str
                                                      utils/ensure-not-blank)
                                      :body-id parsed-location
                                      :location location
                                      :schema (parse-ast body (-> context
                                                                  (assoc :location location)
                                                                  (assoc :parsed-location parsed-location)
                                                                  (assoc :type-hint :type)))}))))))

(defmethod parse-ast :type [node {:keys [location parsed-location is-fragment references] :as context}]
  (debug "Parsing type")
  (let [;; the node can be the string of a type reference if that's the case,
        ;; we build a {:type TypeReference} node to process it
        node (if (and
                  (not (shapes/inline-json-schema? node))
                  (string? node))
               {:type node}
               node)
        shape-context (-> context
                          (assoc :parsed-location parsed-location)
                          (assoc :parse-ast parse-ast))
        shape (if (shapes/inline-json-schema? node)
                (json-schema-shapes/parse-type (keywordize-keys (platform/decode-json node)) shape-context)
                (shapes/parse-type node shape-context))
        type-id (str (get shape "@id") "/wrapper")]
    ;; ParsedType nodes just wrap the JSON-LD description for the shape.
    ;; They should not generate stand-alone nodes in the JSON-LD domain model, the node IS the shape
    (->> (domain/map->ParsedType {:id type-id
                                  :shape shape})
         (common/with-location-meta-from node))))

(defmethod parse-ast :fragment [node {:keys [location parsed-location is-fragment fragments type-hint document-parser]
                                      :or {fragments (atom {})}
                                      :as context}]
  (debug "Parsing included fragment " (syntax/<-location node))
  (let [fragment-location (syntax/<-location node)]
    (let [parsed-fragment (document-parser node context)
          encoded-element (document/encodes parsed-fragment)
          encoded-element-sources (-> encoded-element :sources)
          clean-encoded-element (condp = type-hint
                                  ;; this information is sensitive to the context, can never be in the fragment
                                  :method (-> encoded-element
                                              (assoc :method nil)
                                              (assoc :sources nil))
                                  :resource (-> encoded-element
                                                (assoc  :path nil)
                                                (assoc :sources nil))
                                  encoded-element)
          parsed-location (utils/path-join parsed-location "/includes")
          extends (document/map->ParsedExtends {:id parsed-location
                                                :sources (generate-extend-include-fragment-sources parsed-location fragment-location)
                                                :target fragment-location
                                                :label "!includes"
                                                :arguments []})]
      (swap! fragments (fn [acc]
                         (if (some? (get acc fragment-location))
                           acc
                           (assoc acc fragment-location (assoc parsed-fragment :encodes clean-encoded-element)))))
      (condp = type-hint
        :method  (with-annotations node context
                   (domain/map->ParsedOperation {:id parsed-location
                                                 :method (utils/safe-str (:method encoded-element))
                                                 :sources encoded-element-sources
                                                 :extends [extends]}))
        :resource (with-annotations node context
                    (domain/map->ParsedEndPoint {:id parsed-location
                                                 :path (:path encoded-element)
                                                 :extends [extends]
                                                 :sources encoded-element-sources}))
        (let [properties {:id parsed-location
                          :label "!includes"
                          :target fragment-location}]
          (->>
           (document/map->ParsedIncludes properties)
           (with-annotations node context)
           (common/with-location-meta-from node)))))))

(defmethod parse-ast :undefined [_ _] nil)
