(ns api-modelling-framework.parser.syntax.jsonld
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  #?(:clj (:require [clojure.core.async :refer [<! >! go chan] :as async]
                    [clojure.string :as string]
                    [cemerick.url :as url]
                    [clojure.string :as string]
                    [api-modelling-framework.model.vocabulary :as v]
                    [api-modelling-framework.platform :as platform]))
  #?(:cljs (:require [clojure.string :as string]
                     [cemerick.url :as url]
                     [clojure.string :as string]
                     [clojure.walk :refer [keywordize-keys]]
                     [cljs.core.async :refer [<! >! chan] :as async]
                     [api-modelling-framework.model.vocabulary :as v]
                     [api-modelling-framework.platform :as platform])))

(defn ->id [location]
  (cond
    (some? (string/index-of location "://")) location
    (string/starts-with? location "/")       (str "file://" location)
    (string/starts-with? location ".")       (str "file://" location)
    (string/starts-with? location "..")      (str "file://" location)
    (string/starts-with? location "#")       (str "file://." location)
    :else                                    (str "file://./" location)))

(defn find-references
  ([json]
   (get json v/document:references [])))

(defn fill-references
  ([json acc]
   (let [references (->> (get json v/document:references [])
                         (map (fn [ref] (let [id (get ref "@id")]
                                         (if (some? id)
                                           (get acc id ref)
                                           ref))))
                         (filter #(some? (get % "@id"))))]
     (if (> (count references) 0)
       (assoc json v/document:references references)
       (dissoc json v/document:references)))))

(defn link? [ref]
  (let [keys (keys ref)]
    (every? (fn [property] (= 0 (string/index-of property "@"))) keys)))

(declare parse-file)
(defn resolve-references [refs]
  (go (try (loop [refs refs
                  acc {}]
             (if (empty? refs)
               acc
               (let [ref (first refs)
                     resolved (if (link? ref)
                                (<! (parse-file (get ref "@id")))
                                ref)]
                 (recur (rest refs)
                        (assoc acc (get ref "@id") resolved)))))
           (catch #?(:clj Exception :cljs js/Error) ex
             ex))))

(defn parse-file
  ([location]
   (go (try
         (let [id (->id location)
               data (<! (platform/read-location id))
               parsed (platform/decode-json data)
               references (find-references parsed)
               references-map (<! (resolve-references references))]
           (fill-references parsed references-map))
         (catch #?(:cljs js/Error :clj Exception) ex ex)))))

(defn parse-string
  ([location data]
   (go (try
         (let [id (->id location)
               parsed (platform/decode-json data)
               references (find-references parsed)
               references-map (<! (resolve-references references))]
           (fill-references parsed references-map))
         (catch #?(:cljs js/Error :clj Exception) ex ex)))))
