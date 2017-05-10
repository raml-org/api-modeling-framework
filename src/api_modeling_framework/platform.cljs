(ns api-modeling-framework.platform
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs.core.async  :refer [<! >! chan]]
            [clojure.walk :refer [keywordize-keys]]
            ;; loading Node / web support
            [api_modeling_framework.js-support]))

(enable-console-print!)


(defn error? [x]
  (or (instance? js/Error x)
      (instance? (aget js/global "Error") x)
      (some? (:error x))))

(comment
  (defn <?? [c]
    (let [returned (<!! c)]
      (cond
        (error? returned)          (throw returned)
        (some? (:error returned))  (throw (js/Error. (str (:error returned))))
        :else returned))))

(defn read-location [location]
  (if (or (string/starts-with? location "http://")
          (string/starts-with? location "https://"))
    (let [ch (chan)]
      (go (-> (js/JS_REST location)
              (.then
               (fn [response] (go (>! ch (aget response "entity")))))
              (.catch
               (fn [e] (go (>! ch {:error err} ))))))
      ch)
    (let [location (if (string/starts-with? location "file://")
                     (string/replace location "file://" "")
                     location)
          ch (chan)]
      (go (.readFile js/NODE_FS (first (string/split location #"#"))
                     (fn [e buffer]
                       (go (if (some? e)
                             (>! ch {:error e})
                             (>! ch (.toString buffer)))))))
      ch)))


(defn write-location [location data]
  (let [ch (chan)]
    (go (.writeFile js/NODE_FS first (string/split location #"#")) data
        (fn [e]
          (go (if (some? e))
              (>! ch {:error e})
              (>! ch nil))))
    ch))

(defn decode-json [s] (js->clj (.parse js/JSON s)))

(defn decode-json-ast
  ([location s]
   (try (->> s
             clj->js
             (js/JS_AST location)
             js->clj)
        (catch js/Error e
          (println "ERROR")
          (prn e)
          (println s)
          (js->clj (.parse js/JSON s)))))
  ([s] (decode-json-ast "" s)))

(defn encode-json [s] (.stringify js/JSON (clj->js s)))

(def Err js/Error)

(defn ->clj [x] (let [res (js->clj x)]
                  (if (map? res)
                    (keywordize-keys res)
                    res)))
(defn <-clj [x] (clj->js x))

(defn validate [shape-jsonld payload-jsonld]
  (let [c (chan)
        shape-jsonld (encode-json shape-jsonld)
        payload-jsonld (encode-json payload-jsonld)
        ;;_ (println "SHAPE_JSONLD")
        ;;_ (println shape-jsonld)
        ;;_ (println "DATA_JSONLD")
        ;;_ (println payload-jsonld)
        validated (.validate js/SHACL payload-jsonld "application/ld+json"
                             shape-jsonld "application/ld+json"
                             (fn [e r]
                               (go (if (some? e)
                                     (>! c {:err (js->clj e)})
                                     (>! c (decode-json r))))))]
    c))
