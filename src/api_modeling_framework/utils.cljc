(ns api-modeling-framework.utils
  (:require [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.vocabulary :as v]
            [clojure.string :as string]
            [taoensso.timbre :as timbre #?(:clj :refer :cljs :refer-macros) [log]]))

(defn safe-str [x]
  (cond
    (string? x) x
    (keyword? x) (if (string/index-of (str x) "/")
                   (string/replace-first (str x) ":" "")
                   (name x))
    :else (str x)))

(defn ->bool [x]
  (cond (nil? x)                           false
        #?(:clj (instance? Boolean x)
           :cljs (or (= "true" (str x))
                     (= "false" (str x)))) (if (= "true" (str x)) true false)
        (string? x)                        (if (= (string/lower-case x) "true") true)
        :else                              true))

(defn trace-keys [x] (prn (keys x)) x)
(defn trace [x] (prn x) x)

(def key-orders {"swagger" 0
                 "host" 1
                 "info" 2
                 "x-traits" 3
                 "paths" 4})
(defn swaggify [x]
  (cond
    (string? x) x
    (keyword? x) (safe-str x)
    (map? x) (->> x
                  (mapv (fn [[k v]] [(swaggify k) (swaggify v)]))
                  (sort (fn [[ka va] [kb vb]]
                          (let [pos-a (get key-orders ka 100)
                                pos-b (get key-orders kb 100)]
                            (compare pos-a pos-b))))
                  (reduce (fn [acc [k v]] (assoc acc k v))
                          (sorted-map)))
    (coll? x) (mapv #(swaggify %) x)
    :else x))


(defn ramlify [x]
  (cond
    (string? x) x
    (keyword? x) (safe-str x)
    (map? x) (->> x
                  (mapv (fn [[k v]] [(ramlify k) (ramlify v)]))
                  (sort (fn [[ka va] [kb vb]]
                          (cond
                            (and (string/starts-with? ka "/")
                                 (string/starts-with? kb "/"))  0
                            (string/starts-with? ka "/")        1
                            (= ka "title")                     -1
                            (and (= ka "version")
                                 (or (not= kb "title")))       -1
                            (and (= ka "baseUri")
                                 (or (not= kb "title")
                                     (not= kb "version")))     -1
                            :else                              -1)))
                  (reduce (fn [acc [k v]] (assoc acc k v))
                          (sorted-map)))
    (coll? x) (mapv #(ramlify %) x)
    :else x))

(defn safe-value [x]
  (if (or (string? x) (keyword? x))
    (safe-str x)
    x))

(defn has-class? [m c]
  (let [types (flatten [(get m "@type")])]
    (->> types (some #(= % c)))))


(defn find-tag [source-map tag-id]
  (when (some? source-map)
    (->> (document/tags source-map)
         (filter #(= tag-id
                     (document/tag-id %)))
         first)))


(defn find-value [m property]
  (-> m (get property) first (get "@value")))

(defn find-link [m property]
  (-> m (get property) first (get "@id")))


(defn find-values [m property]
  (-> m (get property) (->> (map #(get % "@value")))))

(defn find-links [m property]
  (-> m (get property) (->> (map #(get % "@id")))))

(defn clean-nils [jsonld]
  (->> jsonld
       (map (fn [[k v]]
              (let [v (if (and (not (map? v))
                               (coll? v))
                        (filter some? v)
                        v)]
                (cond
                  (#{:get :post :put :patch
                     :head :options :delete :set} k) [k v]
                  (nil? v)                           nil
                  (and (coll? v) (empty? v))         nil
                  (and (map? v) (= v {}))            nil
                  :else                              [k v]))))
       (filter some?)
       (into {})))

(defn assoc-value [t m target property]
  (if (some? (property m))
    (assoc t target [{"@value" (safe-value (property m))}])
    t))

(defn assoc-link [t m target property]
  (if (some? (property m))
    (assoc t target [{"@id" (property m)}])
    t))

(defn assoc-values [t m target property]
  (if (some? (property m))
    (assoc t target (map (fn [v] {"@value" (safe-value v)}) (property m)))
    t))

(defn map-values [m property]
  (let [values (->> [(property m)] flatten (filter some?))]
    (if (empty? values)
      []
      (map (fn [v] {"@value" (safe-value v)}) values))))

(defn assoc-object [t m target property mapping]
  (if (some? (property m))
    (if-let [value (mapping (property m))]
      (assoc t target [value])
      t)
    t))

(defn assoc-objects [t m target property mapping]
  (if (some? (property m))
    (assoc t target (map #(mapping %) (property m)))
    t))

(defn extract-nested-resources [node]
  (->> node
       (filter (fn [[k v]]
                 (string/starts-with? (str k) ":/")))
       (map (fn [[k v]]
              {:path (-> k str (string/replace-first ":/" "/"))
               :resource v}))))

(defn extract-jsonld-literal
  ([node property f]
   (let [value (-> node (get property []) first (get "@value"))]
     (if (some? value) (f value) nil)))
  ([node property] (extract-jsonld-literal node property identity)))

(defn extract-jsonld
  ([node property f]
   (let [value (-> node (get property []) first)]
     (if (some? value) (f value) nil)))
  ([node property] (extract-jsonld node property identity)))

(defn alias-chain [alias {:keys [alias-chain]}]
  (if (some? alias-chain) (str (safe-str alias-chain) "." (safe-str alias)) (safe-str alias)))

(defn path-join [base & parts]
  (if-let [next (first parts)]
    (let [base (safe-str base)
          next (safe-str next)
          base (if (= (last base) \/)
                 (->> base drop-last (apply str))
                 base)
          next (if (= (first (into [] next)) \/)
                 next
                 (str "/" next))
          joined (apply path-join (concat [(str base next)] (rest parts)))]
      (string/replace joined  "##" "#"))
    base))

(defn last-component [s]
  (let [maybe-hash (-> s safe-str (string/split #"/") last)]
    (-> maybe-hash (string/split #"#") last)))


(defn annotation->jsonld [data]
  (cond
    (map? data) (->> data
                     (map (fn [[k v]]
                            [(v/anon-shapes-ns (safe-str k)) (annotation->jsonld v)]))
                     (into {})
                     clean-nils)
    (coll? data) (mapv #(annotation->jsonld %) data)
    (nil? data)  nil
    :else        {"@value" data}))

(defn jsonld->annotation [data]
  (cond
    (and
     (map? data)
     (= ["@value"] (keys data))) (get data "@value")

    (map? data)                  (->> data
                                      (map (fn [[k v]]
                                             [(last-component k) (jsonld->annotation v)]))
                                      (into {}))

    (coll? data)                 (mapv (fn [v] (jsonld->annotation v)) data)

    :else                        data))

(defn ensure
  "Makes sure that at least a default value is present in the passed node"
  [n p default-value]
  (if (some? (get n p))
    n
    (assoc n p default-value)))

(defn ensure-not-blank [x]
  (if (and (string? x) (= x "")) nil x))


(defn same-doc? [id1 id2]
  (let [id1 (first (string/split id1 #"#"))
        id2 (first (string/split id2 #"#"))]
    (= id1 id2)))

(defn hash-path [id]
  (str "#" (-> id (string/split #"#") last)))

(defn scalar-shape? [shape]
  (has-class? shape (v/shapes-ns "Scalar")))

(defn nil-shape? [shape]
  (has-class? shape (v/shapes-ns "NilValueShape")))

(defn array-shape? [shape]
  (has-class? shape (v/shapes-ns "Array")))

(defn node-shape? [shape]
  (has-class? shape (v/sh-ns "NodeShape")))

(defn property-shape? [shape]
  (has-class? shape (v/sh-ns "PropertyShape")))

(defn object-no-properties? [x]
  (and (or (nil? (:type x))
           (= "object" (:type x)))
       (nil? (:properties x))))


(defn scalar-range? [property]
  (some? (get property (v/sh-ns "datatype"))))

(defn property-shape->scalar-shape [property]
  {"@type" [(v/shapes-ns "Scalar")]
   (v/sh-ns "datatype") (get property (v/sh-ns "datatype"))
   (v/sh-ns "in") (get property (v/sh-ns "in"))})

(defn array-range? [property]
  (= {"@value" true} (-> property (get (v/shapes-ns "ordered") []) first)))

(defn nil-range? [property]
  (= {"@value" true} (-> property (get (v/shapes-ns "nilValue") []) first)))

(defn nil-shape->property-shape []
  {;; Object properties vs arrays, only one is allowed if it is an object
   (v/sh-ns "maxCount")  [{"@value" 1}]
   ;; we mark it for our own purposes, for example being able to detect
   ;; it easily without checking in property
   (v/shapes-ns "nilValue") [{"@value" true}]
   ;; range of the prop, values have to be shapes:NilValue
   (v/sh-ns "in")     [{"@list" [(v/shapes-ns "NilValue")]}]})

(defn parse-nil-value [{:keys [parsed-location]}]
  {"@id" parsed-location
   "@type" [(v/shapes-ns "NilValueShape") (v/sh-ns "Shape")]})

(defn property-shape->array-shape [property]
  (let [items (cond
                (some? (get property (v/sh-ns "node")))     [(get property (v/sh-ns "node"))]
                (some? (get property (v/sh-ns "datatype"))) [{"@type" [(v/shapes-ns "Scalar")]
                                                              (v/sh-ns "datatype") (get property (v/sh-ns "datatype"))}]
                (some? (get property (v/sh-ns "or")))       (-> property (get (v/sh-ns "or")) (get "@list"))
                :else [])]
    {"@type" [(v/shapes-ns "Array")]
     (v/sh-ns "in") (get property (v/sh-ns "in"))
     (v/shapes-ns "item") items}))

(defn property-shape->node-shape [property]
  (-> property (get (v/sh-ns "node") []) first))

(defn type-reference? [type-string references]
  (get references (keyword type-string)))


(defn type-link? [node references]
  (and (or (= [:type] (keys node))
           (= [:schema] (keys node))
           (= [:$ref] (keys node)))
       (some? (type-reference? (first (vals node)) references))))


(defn link-format? [node]
  (and (map? node)
       (or (= [:type] (keys node))
           (= [:schema] (keys node))
           (= [:$ref] (keys node)))))

(defn ensure-type-property [node]
  (let [type (:type node (:schema node))]
    (-> node
        (dissoc :type)
        (dissoc :schema)
        (assoc :type type))))


(defn node-name->domain-uri [node-name]
  (condp = node-name
    ;; Open API
    "Swagger" v/http:APIDocumentation
    "Info" v/http:APIDocumentation
    "Paths" v/http:APIDocumentation
    "PathItem" v/http:EndPoint
    "Schema" (v/sh-ns "Shape")
    "Operation" v/hydra:Operation
    ;; RAML
    "API" v/http:APIDocumentation
    "DocumentationItem" (v/http-ns "DocumentationItem")
    "Resource" v/http:EndPoint
    "Method" v/hydra:Operation
    "Response" v/http:Response
    "RequestBody" v/http:Request
    "ResponseBody" v/http:Payload
    "TypeDeclaration" (v/sh-ns "Shape")
    "Example" (v/http-ns "Example")
    "ResourceType" (v/http-ns "AbstractEndPoint")
    "Trait" (v/http-ns "AbstractResponse")
    "SecurityScheme" (v/http-ns "SecurityScheme")
    "SecuritySchemeSettings" (v/http-ns "SecuritySettings")
    "AnnotationType" v/document:DomainPropertySchema
    "Library" v/document:Module
    "Overlay" (v/http-ns "AbstractAPIDocumentation")
    "Extension" (v/http-ns "PartialAPIDocumentation")
    nil))

(defn domain-uri->node-name [node-name]
  (condp = node-name
    v/http:APIDocumentation "API"
    (v/http-ns "DocumentationItem") "DocumentationItem"
    v/http:EndPoint "Resource"
    v/hydra:Operation "Method"
    v/http:Response "Response"
    v/http:Request "RequestBody"
    v/http:Payload "ResponseBody"
    (v/sh-ns "Shape") "TypeDeclaration"
    (v/http-ns "Example") "Example"
    (v/http-ns "AbstractEndPoint") "ResourceType"
    (v/http-ns "AbstractResponse") "Trait"
    (v/http-ns "SecurityScheme") "SecurityScheme"
    (v/http-ns "SecuritySettings") "SecuritySchemeSettings"
    v/document:DomainPropertySchema "AnnotationType"
    v/document:Module "Library"
    (v/http-ns "AbstractAPIDocumentation") "Overlay"
    (v/http-ns "PartialAPIDocumentation") "Extension"
    nil))

(defn domain-uri->openapi-node-name [node-name]
  (condp = node-name
    v/http:APIDocumentation "Swagger"
    v/http:EndPoint "PathItem"
    v/hydra:Operation "Operation"
    v/http:Response "Response"
    (v/sh-ns "Shape") "Schema"
    ;; If there's no OpenAPI node name, let's use raml
    (domain-uri->node-name node-name)))
