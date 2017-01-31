(ns api-modelling-framework.parser.syntax.json
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  #?(:clj (:require [clojure.core.async :refer [<! >! go chan] :as async]
                    [clojure.string :as string]
                    [cemerick.url :as url]
                    [api-modelling-framework.platform :as platform]))
  #?(:cljs (:require [clojure.string :as string]
                     [cemerick.url :as url]
                     [clojure.walk :refer [keywordize-keys]]
                     [cljs.core.async :refer [<! >! chan] :as async]
                     [api-modelling-framework.platform :as platform])))

(defn ->int [s]
  #?(:cljs (js/parseInt s)
     :clj  (Integer/parseInt s)))

(defn next-token [tokens]
  (let [token (first tokens)
        token(string/replace token "~1" "/")
        token(string/replace token "~0" "~")]
    token))

(defn- json-pointer* [tokens object]
  (cond
    (nil? object)         object
    (empty? tokens)       object
    (map? object)         (let [token (next-token tokens)
                                next-object (get object token)]
                            (recur (rest tokens) next-object))
    (coll? object)        (let [token (next-token tokens)
                                index (->int token)
                                next-object (nth object index)]
                            (recur (rest tokens) next-object))
    :else                 nil))

(defn json-pointer
  "Returns the pointed object by a JSON pointer, provided a path and a parent document"
  [path object]
  (cond
    (= path "") object
    (= path "/") (json-pointer* [""] object)
    :else (let [path (if (string/starts-with? path "#")
                       (string/replace-first path "#" "")
                       path)
                tokens (string/split path #"/")
                tokens (if (string/ends-with? path "/")
                         (concat tokens [""])
                         tokens)
                tokens (drop 1 tokens)]
            (json-pointer* tokens object))))


(defn ->id [location]
  (cond
    (some? (string/index-of location "://")) location
    (string/starts-with? location "/")       (str "file://" location)
    (string/starts-with? location ".")       (str "file://" location)
    (string/starts-with? location "..")      (str "file://" location)
    (string/starts-with? location "#")       (str "file://." location)
    :else                                    (str "file://./" location)))


(defn join-path [id path]
  (let [id-parts (url/url id)
        path-parts (url/url (->id path))]
    (if (or
         (and (some? (string/index-of path "://"))
              (not= (:protocol path-parts) (:protocol id-parts)))
         (and (not= (:host path-parts) "")
              (not= (:host path-parts) ".")
              (not= (:host path-parts) "..")
              (not= (:host path-parts) ".#")))
      path
      (cond
        (or (= ".#" (:host path-parts))
            (and (= "" (:path path-parts))
                 (some? (:anchor path-parts)))) (str (-> id
                                                         (string/split #"#")
                                                         first)
                                                     "#"
                                                     (:anchor path-parts))
        (and (= "." (:host path-parts))
             (some? (:path path-parts)))  (let [base-path (-> (:path id-parts)
                                                              (string/split #"/")
                                                              (->> (drop-last 1)
                                                                   (string/join "/")))]
                                            (str (-> id
                                                     (string/split #"#")
                                                     first
                                                     (string/replace (:path id-parts) ""))
                                                 base-path
                                                 (:path path-parts)
                                                 (if (:anchor path-parts) (str "#" (:anchor path-parts)) "")))
        (and (= ".." (:host path-parts))
             (some? (:path path-parts)))  (let [base-path (-> (:path id-parts)
                                                              (string/split #"/")
                                                              (->> (drop-last 2)
                                                                   (string/join "/")))]
                                            (str (-> id
                                                     (string/split #"#")
                                                     first
                                                     (string/replace (:path id-parts) ""))
                                                 base-path
                                                 (:path path-parts)
                                                 (if (:anchor path-parts) (str "#" (:anchor path-parts)) "")))

        (= "" (:host path-parts)) (str (string/replace id (:path id-parts) "")
                                       (:path path-parts)
                                       (if (:anchor path-parts) (str "#" (:anchor path-parts)) ""))))))

(defn external-reference? [json]
  (and (some? (get json "$ref"))
       (string? (get json "$ref"))
       (not (string/starts-with? (get json "$ref") "#"))))

(defn find-references
  ([id json] (find-references id json []))
  ([id json acc]
   (let [pending (atom acc)]
     [pending (cond
                (external-reference? json) (let [next-id (join-path id (get json "$ref"))]
                                             (swap! pending #(conj % next-id))
                                             json)
                (map? json)                (let [current-id (if (get json "id")
                                                              (join-path id (get json "id"))
                                                              id)]
                                             (loop [pairs json
                                                    acc {}]
                                               (if (empty? pairs)
                                                 acc
                                                 (let [[k v] (first pairs)
                                                       [processed-acc processed] (find-references current-id v [])]
                                                   (swap! pending #(concat % @processed-acc))
                                                   (recur (rest pairs)
                                                          (assoc acc k processed))))))
                (coll? json)                (->> json
                                                 (mapv #(find-references id % []))
                                                 (mapv (fn [[processed-acc processed]]
                                                         (swap! pending #(concat % @processed-acc))
                                                         processed)))
                :else json)])))

(defn fill-references
  ([id json acc]
   (cond
     (external-reference? json) (let [next-id (join-path id (get json "$ref"))]
                                  (get acc next-id))
     (map? json)                (let [current-id (if (get json "id")
                                                   (join-path id (get json "id"))
                                                   id)]
                                  (loop [pairs json
                                         map-acc{}]
                                    (if (empty? pairs)
                                      map-acc
                                      (let [[k v] (first pairs)]
                                        (recur (rest pairs)
                                               (assoc map-acc k (fill-references current-id v acc)))))))
     (coll? json)                (->> json
                                      (mapv #(fill-references id % acc)))
     :else json)))

(defn absolute-uri? [uri]
  (-> uri
      (string/split #"://")
      last
      (string/starts-with? ".")
      not))

(defn resolve-references [refs]
  (go (try (loop [refs refs
                  acc {}]
             (if (empty? refs)
               acc
               (let [ref (first refs)
                     resolved (platform/decode-json (<! (platform/read-location ref)))
                     pointed (if (string/index-of ref "#")
                               (json-pointer (str "#" (last (string/split ref #"#"))) resolved )
                               resolved)]
                 (recur (rest refs)
                        (assoc acc ref (if (absolute-uri? ref)
                                         {"@location" ref
                                          "id" ref
                                          "@data" pointed}
                                         {"@location" ref
                                          "@data" pointed}))))))
           (catch #?(:clj Exception :cljs js/Error) ex
             ex))))
(defn parse-json [location]
  (go (try
        (let [id (->id location)
              data (<! (platform/read-location id))
              parsed (platform/decode-json data)
              [processed-acc processed] (find-references id parsed)]
          (loop [processed processed
                 processed-acc @processed-acc]
            (if (empty? processed-acc)
              (clojure.walk/keywordize-keys {"@location" id
                                             "@fragment" "root"
                                             "@data" processed})
              (let [references-map (<! (resolve-references processed-acc))
                    processed (fill-references id processed references-map)
                    [processed-acc processed] (find-references id processed)]
                ;; we recur until we have resolved all references
                (recur processed @processed-acc)))))
        (catch #?(:cljs js/Error :clj Exception) ex ex))))
