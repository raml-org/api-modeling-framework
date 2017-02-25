(ns api-modelling-framework.parser.domain.raml
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.raml-types-shapes :as shapes]
            [api-modelling-framework.utils :as utils]
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
   :types #{:root}
   :uriParameters #{:resource}
   :uses #{:root}
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
                        (mapv (fn [[k _]] (get properties-map k (guess-type-from-predicates k))))
                        (filterv some?))
        node-type (first (if (empty? node-types) [] (apply set/intersection node-types)))]
    (or node-type :undefined)))

(defn check-reference
  "Checks if a provided string points to one of the types defined at the APIDocumentation level"
  [node-str {:keys [references]}]
  (if (some? (get references (keyword (utils/safe-str node-str))))
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

(defn generate-parse-node-sources
  "Source map pointing to the incoming node for this AST node"
  [location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/node-parsed")
        node-parsed-tag (document/->NodeParsedTag source-map-id location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [node-parsed-tag])]))

(defn parse-parameters [type location-segment parameters {:keys [location parsed-location is-fragment] :as context}]
  (if (nil? parameters) []
      (->> parameters
           (mapv (fn [[header-name header-value]]

                   (let [header-value (if (string? header-value) {:type header-value} header-value)
                         header-type (get header-value :type "string")
                         header-value (assoc header-value :type header-type)
                         parsed-location (str location "/" location-segment "/" (url/url-encode (utils/safe-str header-name)))
                         location (str location "/" location-segment "/" (url/url-encode (utils/safe-str header-name)))
                         node-parsed-source-map (generate-parse-node-sources location parsed-location)
                         required (extract-scalar (:required header-value))
                         header-shape (shapes/parse-type header-value (-> context
                                                                          (assoc :location location)
                                                                          (assoc :parsed-location parsed-location)))
                         properties {:id parsed-location
                                     :name (utils/safe-str header-name)
                                     :sources node-parsed-source-map
                                     :required required
                                     :parameter-kind type
                                     :shape header-shape}]
                     (if is-fragment
                       (domain/map->ParsedDomainElement {:id parsed-location
                                                         :fragment-node :parsed-parameter
                                                         :properties properties})
                       (domain/map->ParsedParameter properties))))))))

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
    (some? protocols)                  (->> [protocols] flatten (mapv string/lower-case))
    (and (some? baseUri)
         (some? (:protocol
                 (url/url baseUri)))) [(:protocol (url/url baseUri))]
    :else                              nil))


(defn parse-nested-resources [extracted-resources parent-path location parsed-location context]
  (->> extracted-resources ;; this comes from utils/extract-nested-resources it returns a map with {:path :resource} keys
       (mapv (fn [i {:keys [path resource]}]
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
    (flatten [(document/->DocumentSourceMap (str resource-id "/source-map/0") location [node-parsed-tag])
              (document/->DocumentSourceMap (str resource-id "/source-map/1") location [node-parent-tag])
              (mapv (fn [i child-tag]
                      (document/->DocumentSourceMap (str resource-id "/source-map/" (+ i 2)) location [child-tag]))
                    (range 0 (count nested-children-tags))
                    nested-children-tags)])))

(defn generate-inline-fragment-parsed-sources [parsed-location fragment-name fragment-location]
  (let [source-map-id (str parsed-location "/source-map/inline-fragment/" fragment-name)
        inline-fragment-parsed-tag (document/->InlineFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map")
                                   fragment-location
                                   [inline-fragment-parsed-tag])]))

(defn generate-extend-include-fragment-sources [parsed-location fragment-location]
  (let [source-map-id (str parsed-location "/source-map/inline-fragment")
        parsed-tag (document/->ExtendIncludeFragmentParsedTag source-map-id fragment-location)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map")
                                   fragment-location
                                   [parsed-tag])]))

(defn generate-is-trait-sources [trait-name location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/is-trait")
        is-trait-tag (document/->IsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [is-trait-tag])]))

(defn generate-is-type-sources [type-name location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/is-type")
        is-type-tag (document/->IsTypeTag source-map-id type-name)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [is-type-tag])]))


(defn process-traits [node {:keys [location parsed-location] :as context}]
  (debug "Processing " (count (:traits node [])) "traits")
  (let [location (str location "/traits")
        parsed-location (str parsed-location "/traits")
        nested-context (-> context (assoc :location location) (assoc :parsed-location parsed-location))]
    (->> (:traits node {})
         (reduce (fn [acc [trait-name trait-node]]
                   (debug (str "Processing trait " trait-name))
                   (let [fragment-name (url/url-encode (utils/safe-str trait-name))
                         trait-fragment (parse-ast trait-node (-> nested-context
                                                                  (assoc :location location)
                                                                  (assoc :parsed-location parsed-location)
                                                                  (assoc :is-fragment true)
                                                                  (assoc :type-hint :method)))
                         trait-fragment (assoc trait-fragment :method nil) ;; method must be nil, this information cannot be in the fragment
                         trait-fragment (assoc trait-fragment :id (str parsed-location "/" fragment-name))
                         trait-fragment (assoc-in trait-fragment [:properties :id] (str parsed-location "/" fragment-name))
                         sources (or (-> trait-fragment :properties :sources) [])
                         sources (concat sources (generate-is-trait-sources fragment-name
                                                                            (str location "/" fragment-name)
                                                                            (str parsed-location "/" fragment-name)))
                         parsed-trait (assoc-in trait-fragment [:properties :sources] sources)]
                     (assoc acc (keyword trait-name) parsed-trait)))
                 {}))))

(defn process-types [node {:keys [location parsed-location] :as context}]
  (debug "Processing " (count (:types node [])) " types")
  (let [parsed-location (str parsed-location "/types")
        nested-context (-> context (assoc :location location) (assoc :parsed-location parsed-location))]
    (->> (:types node {})
         (reduce (fn [acc [type-name type-node]]
                   (debug (str "Processing type " type-name))
                   (let [type-name     (url/url-encode (utils/safe-str type-name))
                         type-fragment (parse-ast type-node (-> nested-context
                                                                (assoc :location location)
                                                                (assoc :parsed-location parsed-location)
                                                                (assoc :is-fragment false)
                                                                (assoc :type-hint :type)))
                         type-fragment (assoc type-fragment :id (str parsed-location "/" type-name))
                         sources (or (-> type-fragment :sources) [])
                         sources (concat sources (generate-is-type-sources type-name
                                                                           (str location "/" type-name)
                                                                           (str parsed-location "/" type-name)))
                         parsed-type (assoc type-fragment :sources sources)]
                     (assoc acc (keyword type-name) parsed-type)))
                 {}))))

(defn find-extend-tags [{:keys [location parsed-location references] :as context}]
  (->> references
       (mapv (fn [[extend-name parsed-domain-element]]
               (condp = (:fragment-node parsed-domain-element)
                 :trait (generate-inline-fragment-parsed-sources (str parsed-location "/traits")
                                                                 (name extend-name)
                                                                 (:id parsed-domain-element))
                 (generate-inline-fragment-parsed-sources (str parsed-location "/declares")
                                                          (name extend-name)
                                                          (:id parsed-domain-element)))))
       flatten))

(defmethod parse-ast :root [node {:keys [location parsed-location is-fragment references] :as context :or {references {}}}]
  (debug "Parsing RAML root")
  (let [parsed-location (str parsed-location "/api-documentation")
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
        properties {:id parsed-location
                    :sources (concat (generate-parse-node-sources location parsed-location)
                                     children-tags
                                     trait-tags
                                     ;;types-sources
                                     )
                    :name (extract-scalar (:title node))
                    :description (extract-scalar (:description node))
                    :host (base-uri->host (extract-scalar (:baseUri node)))
                    :scheme (root->scheme node)
                    :base-path (base-uri->basepath (extract-scalar (:baseUri node)))
                    :accepts (filterv some? (flatten [(extract-scalar (:mediaType node))]))
                    :content-type (filterv some? (flatten [(:mediaType node)]))
                    :version (extract-scalar (:version node))
                    :provider nil
                    :terms-of-service nil
                    :license nil
                    :endpoints nested-resources}]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id parsed-location
                                        :fragment-node :parsed-api-documentation
                                        :properties properties})
      (domain/map->ParsedAPIDocumentation properties))))

(defn generate-extends-trait-sources [trait-name location parsed-location]
  (let [source-map-id (str parsed-location "/source-map/extend-trait")
        extends-trait-tag (document/->ExtendsTraitTag source-map-id trait-name)]
    [(document/->DocumentSourceMap (str parsed-location "/source-map") location [extends-trait-tag])]))

(defn parse-traits [resource-id node references {:keys [location parsed-location]}]
  (let [traits (flatten [(:is node [])])]
    (->> traits
         (mapv (fn [trait-name]
                 [trait-name (-> references
                                 (get (keyword trait-name)))]))
         (mapv (fn [i [trait-name trait]]
                 (if (some? trait)
                   (let
                       [extend-id (str parsed-location "/extends/" (url/url-encode trait-name))
                        extend-location (str location "/is/" i)
                        node-parsed-source-map (generate-parse-node-sources extend-location extend-id)
                        extends-trait-source-map (generate-extends-trait-sources trait-name extend-location extend-id)]
                     (document/map->ParsedExtends {:id extend-id
                                                   :sources (concat node-parsed-source-map
                                                                    extends-trait-source-map)
                                                   :target (document/id trait)
                                                   :label "trait"
                                                   :arguments []}))
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
        operations (->> node
                        keys
                        (mapv (fn [op] (if-let [operation (#{:get :post :patch :put :delete :head :options} op)]
                                        (assoc (parse-ast (get node operation {})
                                                          (-> context
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
                                      :extends traits})]
    (concat (if is-fragment
              [(domain/map->ParsedDomainElement {:id resource-id
                                                 :fragment-node :parsed-end-point
                                                 :properties properties})]
              [(domain/map->ParsedEndPoint properties)])
            (or nested-resources []))))

(defmethod parse-ast :method [node {:keys [location parsed-location is-fragment method references] :as context}]
  (debug "Parsing method " method)
  (let [method-id (str parsed-location "/" method)
        location (str location "/" method)
        next-context (-> context
                         (assoc :is-fragment false)
                         (assoc :parsed-location method-id)
                         (assoc :location location))
        node-parsed-source-map (generate-parse-node-sources location method-id)
        headers (parse-parameters "header" "headers" (:headers node) next-context)
        query-parameters (parse-parameters "query" "queryParameters" (:queryParameters node) next-context)
        ;; @todo we need to fix the problem with the query-string
        ;;query-string (parse-parameters "query" "queryString" (:queryString node) next-context)
        query-string []
        request-id (str method-id "/request")
        traits (parse-traits method-id node references next-context)
        body (parse-ast (:body node) (-> context
                                         (assoc :is-fragment false)
                                         (assoc :location (str location "/body"))
                                         (assoc :parsed-location (str parsed-location "/body"))
                                         (assoc :type-hint :type)))
        request (domain/map->ParsedRequest (utils/clean-nils {:id request-id
                                                              :sources (generate-parse-node-sources location request-id)
                                                              :parameters (concat  query-parameters query-string)
                                                              :schema body}))
        responses (parse-ast (:responses node {}) (-> next-context
                                                      (assoc :type-hint :responses)))
        properties (-> {:id method-id
                        :sources node-parsed-source-map
                        :method (utils/safe-str method)
                        :name (extract-scalar (:displayName node))
                        :description (extract-scalar (:description node))
                        :scheme (extract-scalar (:protocols node))
                        :headers headers
                        :request request
                        :responses (flatten responses)
                        :extends traits}
                       utils/clean-nils)]
    (if is-fragment
      (domain/map->ParsedDomainElement {:id method-id
                                        :fragment-node :parsed-operation
                                        :properties properties})
      (domain/map->ParsedOperation properties))))

(defmethod parse-ast :responses [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing responses")
  (->> node
       (mapv (fn [[key response]]
               (parse-ast response (-> context
                                       (assoc :location (str location "/responses"))
                                       (assoc :type-hint :response)
                                       (assoc :status-code key)))))))

(defn extract-bodies [node {:keys [location parsed-location] :as context}]
  (let [location (str location "/body")
        parsed-location (str parsed-location "/body")
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

(defmethod parse-ast :response [node {:keys [location parsed-location is-fragment status-code] :as context}]
  (debug "Parsing response " status-code)
  (let [response-id (str parsed-location "/" status-code)
        location (str location "/" status-code)
        status-code (if (integer? status-code) (str status-code) (name status-code))
        node-parsed-source-map (generate-parse-node-sources location response-id)
        headers (parse-parameters "header" "headers" (:headers node) (-> context
                                                                         (assoc :location location)
                                                                         (assoc :parsed-location response-id)))
        properties (-> {:name status-code
                        :status-code status-code
                        :headers headers
                        :description (extract-scalar (:description node))}
                       utils/clean-nils)
        bodies (extract-bodies (:body node) (-> context
                                                (assoc :location location)
                                                (assoc :parsed-location response-id)))]
    (->> bodies
         (mapv (fn [{:keys [media-type body-id location schema] :as data}]
                 (let [media-type-node-sources (if (some? media-type)
                                                 (generate-parse-node-sources location body-id)
                                                 [])]
                   (-> properties
                       (assoc :sources (concat node-parsed-source-map media-type-node-sources))
                       (assoc :id (or body-id response-id))
                       (assoc :content-type [media-type])
                       (assoc :schema schema)
                       utils/clean-nils))))
         (mapv (fn [properties]
                 (if is-fragment
                   (domain/map->ParsedDomainElement {:id response-id
                                                     :fragment-node :parsed-response
                                                     :properties properties})
                   (domain/map->ParsedResponse properties)))))))

(defmethod parse-ast :body-media-type [node {:keys [location parsed-location is-fragment] :as context}]
  (debug "Parsing body media-type")
  (->> node
       (mapv (fn [[media-type body]]
               (let [location (str location "/" (url/url-encode media-type))
                     parsed-location (str parsed-location "/" (url/url-encode media-type))]
                 {:media-type media-type
                  :body-id parsed-location
                  :location location
                  :schema (parse-ast body (-> context
                                              (assoc :location location)
                                              (assoc :parsed-location parsed-location)
                                              (assoc :type-hint :type)))})))))

(defmethod parse-ast :type [node {:keys [location parsed-location is-fragment references] :as context}]
  (debug "Parsing type")
  (if (and (string? node) (not (string/starts-with? node "{")))
    (get references (keyword (utils/safe-str node)))
    (let [type-id (str parsed-location "/type")
          shape (shapes/parse-type node (assoc context :parsed-location type-id))]
      (if is-fragment
        {:id type-id
         :shape shape}
        (domain/map->ParsedType {:id type-id
                                 :shape shape})))))

(defmethod parse-ast :fragment [node {:keys [location parsed-location is-fragment fragments type-hint document-parser]
                                      :or {fragments (atom {})}
                                      :as context}]
  (let [fragment-location (syntax/<-location node)]
    (let [parsed-fragment (document-parser node context)
          encoded-element (document/encodes parsed-fragment)
          encoded-element-sources (-> encoded-element :properties :sources)
          clean-encoded-element (condp = type-hint
                                  ;; this information is sensitive to the context, can never be in the fragment
                                  :method (-> encoded-element
                                              (assoc-in [:properties :method] nil)
                                              (assoc-in [:properties :sources] nil))
                                  :resource (-> encoded-element
                                                (assoc-in [:properties :path] nil)
                                                (assoc-in [:properties :sources] nil))
                                  encoded-element)
          parsed-location (str parsed-location "/includes")
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
        :method  (if is-fragment
                   (domain/map->ParsedDomainElement {:id parsed-fragment
                                                     :fragment-node :parsed-operation
                                                     :properties {:id parsed-location
                                                                  :method (utils/safe-str (-> encoded-element :properties :method))
                                                                  :extends [extends]
                                                                  :sources encoded-element-sources}})
                   (domain/map->ParsedOperation {:id parsed-location
                                                 :method (utils/safe-str (-> encoded-element :properties :method))
                                                 :sources encoded-element-sources
                                                 :extends [extends]}))
        :resource (if is-fragment
                    (domain/map->ParsedDomainElement {:id parsed-fragment
                                                      :fragment-node :parsed-end-point
                                                      :properties {:id parsed-location
                                                                   :path (-> encoded-element :properties :path)
                                                                   :extends [extends]
                                                                   :sources encoded-element-sources}})
                    (domain/map->ParsedEndPoint {:id parsed-location
                                                 :path (-> encoded-element :properties :path)
                                                 :extends [extends]
                                                 :sources encoded-element-sources}))
        (let [properties {:id parsed-location
                          :label "!includes"
                          :target fragment-location}]
          (if is-fragment
            (domain/map->ParsedDomainElement {:id parsed-location
                                              :fragment-node :parsed-includes
                                              :properties properties})
            (document/map->ParsedIncludes properties)))))))

(defmethod parse-ast :undefined [_ _] nil)
