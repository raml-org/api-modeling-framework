(ns api-modelling-framework.platform
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs.core.async  :refer [<! >! chan]]
            [clojure.walk :refer [keywordize-keys]]))

(enable-console-print!)

(defn error? [x]
  (or (instance? js/Error x)
      (instance? (.-Error js/global) x)
      (some? (:error x))))

(comment
  (defn <?? [c]
    (let [returned (<!! c)]
      (cond
        (error? returned)          (throw returned)
        (some? (:error returned))  (throw (js/Error. (str (:error returned))))
        :else returned))))

(defn read-location [location]
  (let [location (if (string/starts-with? location "file://")
                   (string/replace location "file://" "")
                   location)
        ch (chan)]
    (go (.readFile js/NODE_FS (first (string/split location #"#"))
                   (fn [e buffer]
                     (go (if (some? e)
                           (>! ch {:error e})
                           (>! ch (.toString buffer)))))))
    ch))


(defn write-location [location data]
  (let [ch (chan)]
    (go (.writeFile js/NODE_FS first (string/split location #"#")) data
        (fn [e]
          (go (if (some? e))
              (>! ch {:error e})
              (>! ch nil))))
    ch))

(defn decode-json [s] (js->clj (.parse js/JSON s)))

(defn encode-json [s] (.stringify js/JSON (clj->js s)))

(def Err js/Error)

(defn ->clj [x] (let [res (js->clj x)]
                  (if (map? res)
                    (keywordize-keys res)
                    res)))
(defn <-clj [x] (clj->js x))
