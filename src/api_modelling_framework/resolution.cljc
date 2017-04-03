(ns api-modelling-framework.resolution
  (:require [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.model.vocabulary :as v]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn ensure-encoded-fragment [x]
  (try (document/encodes x)
       (catch #?(:clj Exception :cljs js/Error) e
         x)))

(defn merge-declaration [node declaration]
  (cond
    (nil? node) declaration
    (nil? declaration) node
    ;; Both objects have a map value, deep merge
    (and (map? node) (map? declaration)) (reduce (fn [node property]
                                                   (let [declaration-value (get declaration property)
                                                         node-value (get node property)]
                                                     (assoc node property (merge-declaration node-value declaration-value))))
                                                 node
                                                 (keys declaration))
    ;; Collections compact uniq
    (and (coll? node) (coll? declaration)) (->> (concat node declaration) set (into []))
    (coll? node)                           (->> (concat node [declaration]) set (into []))
    (coll? declaration)                    (->> (concat [node] declaration) set (into []))
    ;; If value defined in both objects, node value overwrites declaration value
    :else node))

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

(defmulti resolve (fn [model ctx] (resolve-dispatch-fn model ctx)))

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
    (resolve (merge-declaration (assoc x :extends nil) fragment) ctx)
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
         (mapv #(resolve % ctx)))))

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

(defmethod resolve :Document [model ctx]
  (debug "Resolving Document " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve fragment ctx))]))
                       (into {}))
        types-fragments (compute-types (vals fragments) [])
        declarations (->> (document/declares model)
                          (mapv (fn [declaration]
                                  [(document/id declaration) (ensure-encoded-fragment
                                                              (resolve declaration (-> ctx
                                                                                       (assoc :fragments fragments)
                                                                                       (assoc :document model)
                                                                                       (assoc :types types-fragments)
                                                                                       (assoc domain/APIDocumentation (document/encodes model)))))]))
                          (into {}))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :declarations declarations)
                                                              (assoc :fragments fragments)
                                                              (assoc :types (compute-types (vals fragments) (vals declarations)))))))))


(defmethod resolve document/Fragment [model ctx]
  (debug "Resolving Fragment " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve fragment ctx))]))
                       (into {}))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :fragments fragments)
                                                              (assoc :types (compute-types fragments []))))))))


(defmethod resolve domain/APIDocumentation [model ctx]
  (debug "Resolving APIDocumentation " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        endpoints (mapv #(resolve % (-> ctx (assoc domain/APIDocumentation model))) (domain/endpoints model))]
    (domain/map->ParsedAPIDocumentation
     (-> {:id (document/id model)
          :name (document/name model)
          :version (domain/version model)
          :host (domain/host model)
          :endpoints endpoints}
         utils/clean-nils))))

(defmethod resolve domain/EndPoint [model ctx]
  (debug "Resolving EndPoint " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        traits (or (document/extends model) [])
        operations (->> (domain/supported-operations model)
                        (mapv #(let [op-traits (:extends %)]
                                 (assoc % :extends (concat op-traits traits))))
                        (mapv #(resolve % (-> ctx (assoc domain/EndPoint model)))))]
    (domain/map->ParsedEndPoint
     (-> {:id (document/id model)
          :name (document/name model)
          :path (compute-path model ctx)
          :supported-operations operations}
         utils/clean-nils))))

(defmethod resolve domain/Operation [model ctx]
  (debug "Resolving Operation " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Operation model)
        ;; we need to merge traits before resolving the resulting structure
        traits (mapv #(resolve % ctx) (document/extends model))
        model (reduce (fn [acc trait] (merge-declaration acc trait))
                      model traits)
        ;; reset the ctx after merging traits
        ctx (assoc ctx domain/Operation model)
        request (resolve (domain/request model) ctx)
        responses (mapv #(resolve % ctx) (domain/responses model))]
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

(defmethod resolve :Request [model ctx]
  (debug "Resolving Request " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx :Request model)
        parameters (mapv #(resolve % ctx) (domain/parameters model))
        headers (compute-headers model ctx)
        payloads (map #(resolve % ctx) (domain/payloads model))]
    (domain/map->ParsedRequest
     (-> {:id (document/id model)
          :name (document/name model)
          :parameters parameters
          :headers headers
          :payloads payloads}
         utils/clean-nils))))

(defmethod resolve domain/Parameter [model ctx]
  (debug "Resolving Parameter " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Parameter model)
        shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedParameter
     (-> {:id (document/id model)
          :name (document/name model)
          :parameter-kind (domain/parameter-kind model)
          :required (domain/required model)
          :shape (resolve shape ctx)}
         utils/clean-nils))))

(defmethod resolve domain/Type [model ctx]
  (debug "Resolving Type " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedType
     (-> {:id (document/id model)
          :name (document/name model)
          :shape (resolve shape ctx)}))))

(defmethod resolve domain/Response [model ctx]
  (debug "Resolving Response " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Response model)
        payloads (map #(resolve % ctx) (domain/payloads model))]
    (domain/map->ParsedResponse
     (-> {:id (document/id model)
          :name (document/name model)
          :status-code (domain/status-code model)
          :payloads payloads
          :headers (compute-headers model ctx)}
         utils/clean-nils))))

(defmethod resolve domain/Payload [model ctx]
  (let [model (ensure-applied-fragment model ctx)
        ctx (assoc ctx domain/Parameter model)
        schema (domain/schema model)]
    (domain/map->ParsedPayload
     (-> {:id (document/id model)
          :name (document/name model)
          :media-type (domain/media-type model)
          :schema (resolve schema ctx)}
         utils/clean-nils))))

(defmethod resolve document/Includes [model {:keys [fragments declarations] :as ctx}]
  (debug "Resolving Includes " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        fragment-target (document/target model)
        fragment (get fragments fragment-target)
        declaration (get declarations fragment-target)
        fragment (or fragment declaration)]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in include relationship " (document/id model))))
      (resolve fragment ctx))))

(defmethod resolve document/Extends [model {:keys [declarations fragments] :as ctx}]
  (debug "Resolving Extends " (document/id model))
  (let [model (ensure-applied-fragment model ctx)
        ;; fragment can be in a declaration, for example a trait
        fragment-extended (get declarations (document/target model))
        ;; can also be in the list of included fragments for resources/methods
        fragment-extended-included (get fragments (document/target model))
        fragment (or fragment-extended fragment-extended-included)]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in extend relationship " (document/id model))))
      (resolve fragment ctx))))

(defmethod resolve nil [_ _]
  (debug "Resolving nil value ")
  nil)

(defmethod resolve :unknown [m _]
  (debug "Resolving nil value")m)

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
                                                (let [property-range (get property (v/shapes-ns "range"))]
                                                  (assoc property (v/shapes-ns "range")
                                                         (mapv #(resolve-type % ctx)
                                                               property-range))))))))

(defn process-arrray-type [type ctx]
  (assoc type (v/shapes-ns "item") (mapv #(resolve-type % ctx)
                                         (get type (v/shapes-ns "item")))))

;;(some? (type-reference? type ctx))  (resolve-type (type-reference? type ctx) ctx)

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
    ;;q(println "\n\nRESOLVING: ")
    ;;q(clojure.pprint/pprint type)
    ;;q(println "RESOLVED")
    ;;q(clojure.pprint/pprint final)
    ;;q(println "\n\n")
    final))

(defmethod resolve :Type [m ctx]
  (debug "Resolving type " (get m "@id"))
  (resolve-type m ctx))
