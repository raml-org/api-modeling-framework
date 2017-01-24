(ns raml-framework.platform
  (:require [clojure.core.async :refer [go >! <!! <! thread chan]]
            [clojure.string :as string]
            [cheshire.core :as json]))

(comment
  (defn <?? [c]
    (let [returned (<!! c)]
      (cond
        (instance? Throwable returned) (throw (Exception. returned))
        (some? (:error returned))      (throw (Exception. (str (:error returned))))
        :else returned))))


(defmacro async [s body]
  `(let [~s (fn [] true)
         res# ~body
         finalres# (<!! res#)]
     (when (some? (:err finalres#))
       (throw (Exception. (str (:err finalres#)))))
     (when (instance? Throwable finalres#)
       (throw finalres#))))

(defn read-location [location]
  (let [location (if (string/starts-with? location "file://")
                   (string/replace location "file://" "")
                   location)]
    (go (try (slurp (first (string/split location #"#")))
             (catch Exception ex
               ex)))))

(defn decode-json [s]
  (json/parse-string s))

(def Err Exception)
