(ns m9dl.platform
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async  :refer [<! >! chan]]
            [clojure.walk :refer [keywordize-keys]]))

(def fs (nodejs/require "fs"))
(enable-console-print!)

(defn error? [x]
  (or (instance? js/Error x)
      (instance? (.-Error js/global) x)))

(defn <?? [c]
  (let [returned (<!! c)]
    (cond
      (error? returned)          (throw returned)
      (some? (:error returned))  (throw (js/Error. (str (:error returned))))
      :else returned)))


(defn read-location [location]
  (let [ch (chan)
        location (if (string/starts-with? location "file://")
                   (string/replace location "file://" "")
                   location)]
    (.readFile (first (string/split location #"#"))
               (fn [e buffer]
                 (go (if (some? e)
                       (>! ch (:error e))
                       (>! ch (.toString buffer))))))
    ch))

(defn decode-json [s] (js->clj (.parse js/JSON s)))

(def Err js/Error)0
