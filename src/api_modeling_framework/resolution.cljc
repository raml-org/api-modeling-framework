(ns api-modeling-framework.resolution
  (:require [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.utils :as utils]
            [api-modeling-framework.model.vocabulary :as v]
            [clojure.string :as string]))

(defn ensure-encoded-fragment [x]
  (try (document/encodes x)
       (catch #?(:clj Exception :cljs js/Error) e
         x)))


(declare merge-declaration*)

(defn should-group? [coll]
  (cond
    ;; something that is not a collection
    (or (nil? coll) (map? coll) (not (coll? coll))) nil
    ;; empty collection
    (nil? (first coll))                             nil
    :else ;; proper collections
    (let [elem (first coll)]
      ;; when merging collections for these model elements
      ;; group by key to do the merging instead of just
      ;; computing the set

      ;; Please be careful moving this logic into a map
      ;; of protocols -> properties
      ;; Clojurescript satisfies? fails if the protocol
      ;; is not directly used, and a var is tried instead
      (cond
        (satisfies? domain/EndPoint elem)  #(:path %)
        (satisfies? domain/Operation elem) #(:method %)
        (satisfies? domain/Response elem)  #(:status-code %)
        (satisfies? domain/Payload elem)   #(:media-type %)
        (utils/property-shape? elem)       #(-> % (get (v/sh-ns "path") []) first (get "@id"))
        :else                              nil))))

(defn shapes? [x]
  (and (coll? x)
       (> (count x) 1)
       (some? (get (first x) "@type"))))

(defn merge-shapes [shapes]
  ;; make sure that we don't get duplicated type information about the type when merging
  ;; @todo review this when adding unions
  (reduce (fn [acc next-shape]
            (cond
              (utils/or-shape? next-shape) (merge (dissoc acc (v/sh-ns "datatype"))
                                                  next-shape)
              (utils/or-shape? acc)        (merge (-> acc
                                                      (dissoc (v/sh-ns "or"))
                                                      (dissoc (v/shapes-ns "is-number")))
                                                  next-shape)
              :else                         (merge acc next-shape)))
          (first shapes)
          (rest shapes)))

(defn group
  ([coll]
   (let [grouping-fn (should-group? coll)]
     (group-by grouping-fn coll)))
  ([coll-nodes coll-declarations]
   (let [group-nodes (group coll-nodes)
         group-declaration (group coll-declarations)]
     (merge-with concat group-nodes group-declaration))))

(defn merge-group [elems]
  (reduce (fn [acc elem]
            (merge-declaration* acc elem))
          (first elems)
          (rest elems)))

(defn merge-colls [node declaration]
  (if (should-group? node)
    ;; group and merge
    (let [grouped (group node declaration)
          res (->> grouped
                   (map (fn [[_ elems]]
                          (if (shapes? elems)
                            (merge-shapes elems)
                            (merge-group elems)))))]
      res)
    ;; merge by value-set
    (->> (concat node declaration) set (into []))))

(defn ensure-abstract [declaration]
  (cond
    ;; traits                                    ;; we must remove the name with the label for the trait
    (satisfies? domain/Operation declaration)    (assoc declaration :name nil)
    :else                                        declaration))

(defn merge-declaration* [node declaration]
  (cond
    (nil? node)                            declaration
    (nil? declaration)                     node
    ;; Both objects have a map value, deep merge
    (and (map? node) (map? declaration))   (reduce (fn [node property]
                                                     (let [declaration (ensure-abstract declaration)
                                                           declaration-value (get declaration property)
                                                           node-value (get node property)]
                                                       (assoc node property (merge-declaration* node-value declaration-value))))
                                                   node
                                                   (keys declaration))
    ;; Collections compact uniq
    (and (coll? node) (coll? declaration)) (merge-colls node declaration)
    (coll? node)                           (->> (concat node [declaration]) set (into []))
    (coll? declaration)                    (->> (concat [node] declaration) set (into []))
    ;; If value defined in both objects, node value overwrites declaration value
    :else                                  node))

(defn merge-declaration [node declaration]
  (let [res (merge-declaration* node declaration)]
    res))

(defn resolve-dispatch-fn [model ctx]
  (let [dispatched (cond
                     (some? (get model "@type"))                 :Type

                     (and (satisfies? document/Fragment model)
                          (satisfies? document/Module model))    :Document

                     (satisfies? document/Fragment model)        document/Fragment

                     (satisfies? domain/APIDocumentation model)  domain/APIDocumentation

                     (satisfies? domain/EndPoint model)          domain/EndPoint

                     (satisfies? domain/Operation model)         domain/Operation

                     (and (satisfies? domain/PayloadHolder model)
                          (satisfies? domain/HeadersHolder model)
                          (satisfies? domain/ParametersHolder model)
                          (satisfies? document/Node model))          :Request

                     (satisfies? domain/Parameter model)         domain/Parameter

                     (satisfies? domain/Payload model)           domain/Payload

                     (satisfies? domain/Type model)              domain/Type

                     (satisfies? domain/Response model)          domain/Response

                     (satisfies? document/Extends model)         document/Extends

                     (satisfies? document/Includes model)        document/Includes

                     (nil? model)                                nil

                     :else                                       :unknown)]
    dispatched))

(defmulti resolve-domain-element (fn [model ctx] (resolve-dispatch-fn model ctx)))

(defn extended-included-fragment [x fragments]
  (if-let [extends (first (document/extends x))]
    (let [tags (document/find-tag extends document/extend-include-fragment-parsed-tag)]
      (if (empty? tags)
        nil
        (let[location (document/target extends)
             fragment (get fragments location)]
          (if (nil? fragment)
            (throw (new #?(:clj Exception :cljs js/Error)
                        (str "Cannot find fragment " location " in include relationship " (document/id x))))
            fragment))))
    nil))


(defn ensure-applied-fragment [x {:keys [fragments] :as ctx}]
  (if-let [fragment (extended-included-fragment x fragments)]
    (resolve-domain-element (merge-declaration (assoc x :extends nil) fragment) ctx)
    x))

(defn compute-path [model ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)
        base-path (or (domain/base-path api-documentation) "")
        path (domain/path model)]
    (string/replace (str base-path path) "//" "/")))

(defn compute-host [ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)]
    (domain/host api-documentation)))

(defn compute-scheme [model ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)
        base-scheme (if (some? api-documentation)
                      (or (domain/scheme api-documentation) nil)
                      nil)
        scheme (domain/scheme model)]
    (or scheme base-scheme)))

(defn compute-headers [model ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)
        base-headers (if (some? api-documentation)
                       (or (domain/headers api-documentation) [])
                       [])
        headers (or (domain/headers model) [])]
    (->> (concat headers base-headers)
         (reduce (fn [acc h] (assoc acc (document/name h) h)) {})
         vals
         (mapv #(resolve-domain-element % ctx)))))

(defn compute-accepts [model ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)
        base-accepts (if (some? api-documentation)
                       (or
                        (domain/accepts api-documentation) [])
                       nil)
        accepts(or (domain/accepts model) [])]
    (or accepts base-accepts)))

(defn compute-content-type [model ctx]
  (let [api-documentation (get ctx domain/APIDocumentation)
        base-content-type (if (some? api-documentation)
                            (or (domain/content-type api-documentation) [])
                            nil)
        content-type(or (domain/content-type model) [])]
    (or content-type base-content-type)))

(defn compute-types [fragments declarations]
  (let [declarations (->> fragments
                          (reduce (fn [acc fragment]
                                    (if (satisfies? document/Module fragment)
                                      (concat acc (document/declares fragment))
                                      acc))
                                  declarations))
        shapes(->> declarations
                   (mapv (fn [declaration]
                           (if (satisfies? domain/Type declaration)
                             (domain/shape declaration)
                             nil)))
                   (filterv some?))]
    (->> shapes
         (mapv (fn [shape] [(get shape "@id") shape]))
         (into {}))))

(defmethod resolve-domain-element :Document [model ctx]
  (utils/debug "Resolving Document " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve-domain-element fragment ctx))]))
                       (into {}))
        library-declarations (->> (document/references model)
                                  (filter #(satisfies? document/Module %))
                                  (map #(document/declares %))
                                  (filter some?)
                                  (apply concat))
        types-fragments (compute-types (vals fragments) [])
        declarations (->> (concat (document/declares model)
                                  library-declarations)
                          (mapv (fn [declaration]
                                  [(document/id declaration) (ensure-encoded-fragment
                                                              (resolve-domain-element declaration (-> ctx
                                                                                       (assoc :fragments fragments)
                                                                                       (assoc :document model)
                                                                                       (assoc :types types-fragments)
                                                                                       (assoc domain/APIDocumentation (document/encodes model)))))]))
                          (into {}))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve-domain-element (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :declarations declarations)
                                                              (assoc :fragments fragments)
                                                              (assoc :types (compute-types (vals fragments) (vals declarations)))))))))


(defmethod resolve-domain-element document/Fragment [model ctx]
  (utils/debug "Resolving Fragment " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve-domain-element fragment ctx))]))
                       (into {}))
        library-declarations (->> (document/references model)
                                  (filter #(satisfies? document/Module %))
                                  (map #(document/declares %))
                                  (filter some?)
                                  (apply concat))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve-domain-element (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :fragments fragments)
                                                              (assoc :declarations library-declarations)
                                                              (assoc :types (compute-types fragments []))))))))


(defmethod resolve-domain-element domain/APIDocumentation [model ctx]
  (utils/debug "Resolving APIDocumentation " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        endpoints (mapv #(resolve-domain-element % (-> ctx (assoc domain/APIDocumentation model))) (domain/endpoints model))]
    (domain/map->ParsedAPIDocumentation
     (-> {:id (document/id model)
          :name (document/name model)
          :version (domain/version model)
          :host (domain/host model)
          :endpoints endpoints}
         utils/clean-nils))))

(defmethod resolve-domain-element domain/EndPoint [model ctx]
  (utils/debug "Resolving EndPoint " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        traits (or (document/extends model) [])
        operations (->> (domain/supported-operations model)
                        (mapv #(let [op-traits (:extends %)]
                                 (assoc % :extends (concat op-traits traits))))
                        (mapv #(resolve-domain-element % (-> ctx (assoc domain/EndPoint model)))))]
    (domain/map->ParsedEndPoint
     (-> {:id (document/id model)
          :name (document/name model)
          :path (compute-path model ctx)
          :supported-operations operations}
         utils/clean-nils))))

(defmethod resolve-domain-element domain/Operation [model ctx]
  (utils/debug "Resolving Operation " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Operation model)
        ;; we need to merge traits before resolving the resulting structure
        traits (mapv #(resolve-domain-element % ctx) (document/extends model))
        model (reduce (fn [acc trait]
                        (merge-declaration acc trait))
                      model traits)
        ;; reset the ctx after merging traits
        ctx (assoc ctx domain/Operation model)
        request (resolve-domain-element (domain/request model) ctx)
        responses (mapv #(resolve-domain-element % ctx) (domain/responses model))]
    (domain/map->ParsedOperation
     (-> {:id (document/id model)
          :name (document/name model)
          :method (domain/method model)
          :request request
          :scheme(compute-scheme model ctx)
          :responses responses
          :accepts (compute-accepts model ctx)
          :content-type (compute-content-type model ctx)}
         utils/clean-nils))))

(defmethod resolve-domain-element :Request [model ctx]
  (utils/debug "Resolving Request " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx :Request model)
        parameters (mapv #(resolve-domain-element % ctx) (domain/parameters model))
        headers (compute-headers model ctx)
        payloads (map #(resolve-domain-element % ctx) (domain/payloads model))]
    (domain/map->ParsedRequest
     (-> {:id (document/id model)
          :name (document/name model)
          :parameters parameters
          :headers headers
          :payloads payloads}
         utils/clean-nils))))

(defmethod resolve-domain-element domain/Parameter [model ctx]
  (utils/debug "Resolving Parameter " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Parameter model)
        shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedParameter
     (-> {:id (document/id model)
          :name (document/name model)
          :parameter-kind (domain/parameter-kind model)
          :required (domain/required model)
          :shape (resolve-domain-element shape ctx)}
         utils/clean-nils))))

(defmethod resolve-domain-element domain/Type [model ctx]
  (utils/debug "Resolving Type " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedType
     (-> {:id (document/id model)
          :name (document/name model)
          :shape (resolve-domain-element shape ctx)}))))

(defmethod resolve-domain-element domain/Response [model ctx]
  (utils/debug "Resolving Response " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Response model)
        payloads (map #(resolve-domain-element % ctx) (domain/payloads model))]
    (domain/map->ParsedResponse
     (-> {:id (document/id model)
          :name (document/name model)
          :status-code (domain/status-code model)
          :payloads payloads
          :headers (compute-headers model ctx)}
         utils/clean-nils))))

(defmethod resolve-domain-element domain/Payload [model ctx]
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Parameter model)
        schema (domain/schema model)]
    (domain/map->ParsedPayload
     (-> {:id (document/id model)
          :name (document/name model)
          :media-type (domain/media-type model)
          :schema (resolve-domain-element schema ctx)}
         utils/clean-nils))))

(defmethod resolve-domain-element document/Includes [model {:keys [fragments declarations] :as ctx}]
  (utils/debug "Resolving Includes " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragment-target (document/target model)
        fragment (get fragments fragment-target)
        declaration (get declarations fragment-target)
        fragment (or fragment declaration)]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in include relationship " (document/id model))))
      (resolve-domain-element fragment ctx))))

(defmethod resolve-domain-element document/Extends [model {:keys [declarations fragments] :as ctx}]
  (utils/debug "Resolving Extends " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ;; fragment can be in a declaration, for example a trait
        fragment-extended (get declarations (document/target model))
        ;; can also be in the list of included fragments for resources/methods
        fragment-extended-included (get fragments (document/target model))
        fragment (or fragment-extended fragment-extended-included)]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in extend relationship " (document/id model))))
      (resolve-domain-element fragment ctx))))

(defmethod resolve-domain-element nil [_ _]
  (utils/debug "Resolving nil value ")
  nil)

(defmethod resolve-domain-element :unknown [m _]
  (utils/debug "Resolving nil value")m)

(defn scalar-type? [type] (->> type
                               (get type "@type")
                               (filter (fn [type] (= type (v/shapes-ns "Scalar"))))
                               first
                               some?))

(defn array-type? [type] (->> type
                              (get type "@type")
                              (filter (fn [type] (= type (v/shapes-ns "Array"))))
                              first
                              some?))

(defn object-type? [type] (->> type
                              keys
                              (filter (fn [type] (= type (v/sh-ns "property"))))
                              first
                              some?))

;; (first (get type "@type"))
(defn type-reference? [type-id {:keys [fragments declarations types] :as ctx}]
  (let [res (or (get fragments type-id)
                (get declarations type-id)
                (get types type-id)
                nil)]
    ;; fragments don't store the type but the Type node,
    ;; we need to extract it
    (or (:shape res)
        res)))

(declare resolve-type)
(defn process-object-type [type ctx]
  (assoc type (v/sh-ns "property") (->> (get type (v/sh-ns "property") [])
                                        (mapv (fn [property]
                                                (let [property-range (get property (v/sh-ns "node"))]
                                                  (if (some? property-range)
                                                    (assoc property (v/sh-ns "node")
                                                           (mapv #(resolve-type % ctx)
                                                                 property-range))
                                                    property)))))))

(defn process-arrray-type [type ctx]
  (assoc type (v/shapes-ns "item") (mapv #(resolve-type % ctx)
                                         (get type (v/shapes-ns "item")))))

(defn maybe-ref? [type]
  (let [inherits (get type (v/shapes-ns "inherits") [])
        properties (get type (v/sh-ns "property") [])]
    (and (= 1 (count inherits))
         (= 0 (count properties)))))

(defn extract-ref [type]
  (let [name (first (get type v/sorg:name []))
        inherited (first (get type (v/shapes-ns "inherits") []))
        name (if (nil? name) (get inherited v/sorg:name) [name])]
    (assoc inherited v/sorg:name name)))

(defn check-inheritance [type ctx]
  (let [super-types (get type (v/shapes-ns "inherits"))]
    (if (some? super-types)
      (let [inherited (assoc type (v/shapes-ns "inherits") (mapv (fn [super-type]
                                                                   (cond
                                                                     (and (map? super-type)
                                                                          (get super-type "@id")
                                                                          (type-reference? (get super-type "@id") ctx))  (resolve-type (type-reference? (get super-type "@id") ctx) ctx)
                                                                     (map? super-type)                                   (resolve-type super-type ctx)
                                                                     (type-reference? super-type ctx)                    (resolve-type (type-reference? super-type ctx) ctx)
                                                                     :else super-type))
                                                                 super-types))]
        (if (maybe-ref? inherited)
          (extract-ref inherited)
          inherited))
      type)))

(defn resolve-type [type ctx]
  (let [resolved-type (cond
                        (nil? type)         type

                        (scalar-type? type) type

                        (array-type? type)  (process-arrray-type type ctx)

                        (object-type? type) (process-object-type type ctx)

                        :else type)
        final (check-inheritance resolved-type ctx)]
    ;;(println "\n\nRESOLVING: ")
    ;;(clojure.pprint/pprint type)
    ;;(println "RESOLVED")
    ;;(clojure.pprint/pprint final)
    ;;(println "\n\n")
    final))

(defmethod resolve-domain-element :Type [m ctx]
  (utils/debug "Resolving type " (get m "@id"))
  (resolve-type m ctx))
