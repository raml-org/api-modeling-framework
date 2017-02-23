(ns api-modelling-framework.resolution
  (:require [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]
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
  (cond
    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))    :Document

    (satisfies? document/Fragment model)        document/Fragment

    (satisfies? domain/APIDocumentation model)  domain/APIDocumentation

    (satisfies? domain/EndPoint model)          domain/EndPoint

    (satisfies? domain/DomainElement model)     domain/DomainElement

    (satisfies? domain/Operation model)         domain/Operation

    (satisfies? domain/Request model)           domain/Request

    (satisfies? domain/Parameter model)         domain/Parameter

    (satisfies? domain/Type model)              domain/Type

    (satisfies? domain/Response model)          domain/Response

    (satisfies? document/Extends model)         document/Extends

    (satisfies? document/Includes model)        document/Includes

    (nil? model)                                nil

    :else                                       :unknown))

(defmulti resolve (fn [model ctx] (resolve-dispatch-fn model ctx)))


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
                       api-documentation)
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

(defmethod resolve :Document [model ctx]
  (debug "Resolving Document " (document/id model))
  (let [fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve fragment ctx))]))
                       (into {}))
        declarations (->> (document/declares model)
                          (mapv (fn [declaration]
                                  [(document/id declaration) (ensure-encoded-fragment
                                                              (resolve declaration (-> ctx
                                                                                       (assoc :fragments fragments)
                                                                                       (assoc :document model)
                                                                                       (assoc domain/APIDocumentation (document/encodes model)))))]))
                          (into {}))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :declarations declarations)
                                                              (assoc :fragments fragments)))))))


(defmethod resolve document/Fragment [model ctx]
  (debug "Resolving Fragment " (document/id model))
  (let [fragments (->> (document/references model)
                       (mapv (fn [fragment]
                               [(document/location fragment) (ensure-encoded-fragment
                                                              (resolve fragment ctx))]))
                       (into {}))]
    (-> model
        (assoc :resolved true)
        (assoc :encodes (resolve (document/encodes model) (-> ctx
                                                              (assoc :document model)
                                                              (assoc :fragments fragments)))))))


(defmethod resolve domain/DomainElement [model ctx]
  (debug "Resolving DomainElement " (document/id model))
  (resolve (domain/to-domain-node model) ctx))

(defmethod resolve domain/APIDocumentation [model ctx]
  (debug "Resolving APIDocumentation " (document/id model))
  (let [endpoints (mapv #(resolve % (-> ctx (assoc domain/APIDocumentation model))) (domain/endpoints model))]
    (domain/map->ParsedAPIDocumentation
     (-> {:id (document/id model)
          :name (document/name model)
          :version (domain/version model)
          :host (domain/host model)
          :endpoints endpoints}
         utils/clean-nils))))

(defmethod resolve domain/EndPoint [model ctx]
  (debug "Resolving EndPoint " (document/id model))
  (let [traits (or (document/extends model) [])
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
  (let [ctx (assoc ctx domain/Operation model)
        ;; we need to merge traits before resolving the resulting structure
        traits (mapv #(resolve % ctx) (document/extends model))
        model (reduce (fn [acc trait] (merge-declaration acc trait)) model traits)
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
          :headers (compute-headers model ctx)}
         utils/clean-nils))))

(defmethod resolve domain/Request [model ctx]
  (debug "Resolving Request " (document/id model))
  (let [ctx (assoc ctx domain/Request model)
        parameters (mapv #(resolve % ctx) (domain/parameters model))
        schema (resolve (domain/schema model) ctx)]
    (domain/map->ParsedRequest
     (-> {:id (document/id model)
          :name (document/name model)
          :parameters parameters
          :schema schema}
         utils/clean-nils))))

(defmethod resolve domain/Parameter [model ctx]
  (debug "Resolving Parameter " (document/id model))
  (let [ctx (assoc ctx domain/Parameter model)
        shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedParameter
     (-> {:id (document/id model)
          :name (document/name model)
          :parameter-kind (domain/parameter-kind model)
          :required (domain/required model)
          :shape shape}
         utils/clean-nils))))

(defmethod resolve domain/Type [model ctx]
  (debug "Resolving Type " (document/id model))
  (let [shape (domain/shape model)] ;; @todo we need to compute the canonical form of the shape
    (domain/map->ParsedType
     (-> {:id (document/id model)
          :name (document/name model)
          :shape shape}))))

(defmethod resolve domain/Response [model ctx]
  (debug "Resolving Response " (document/id model))
  (let [ctx (assoc ctx domain/Response model)
        schema (resolve (domain/schema model) ctx)]
    (domain/map->ParsedResponse
     (-> {:id (document/id model)
          :name (document/name model)
          :status-code (domain/status-code model)
          :content-type (compute-content-type model ctx)
          :schema schema
          :headers (compute-headers model ctx)}
         utils/clean-nils))))

(defmethod resolve document/Includes [model {:keys [fragments] :as ctx}]
  (debug "Resolving Includes " (document/id model))
  (let [fragment (get fragments (document/target model))]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in include relationship " (document/id model))))
      (resolve fragment ctx))))

(defmethod resolve document/Extends [model {:keys [declarations] :as ctx}]
  (debug "Resolving Extends " (document/id model))
  (let [fragment (get declarations (document/target model))]
    (if (nil? fragment)
      (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find fragment " (document/target model) " in extend relationship " (document/id model))))
      (resolve fragment ctx))))

(defmethod resolve nil [_ _]
  (debug "Resolving nil value ")
  nil)

(defmethod resolve :unknown [m _]
  (debug "Resolving nil value")m)
