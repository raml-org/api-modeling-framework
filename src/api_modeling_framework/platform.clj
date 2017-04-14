(ns api-modeling-framework.platform
  (:require [clojure.core.async :refer [go >! <!! <! thread chan]]
            [clojure.string :as string]
            [cheshire.core :as json]))

(defn error? [x]
  (or (instance? Exception x)
      (some? (:error x))))

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
               {:error ex})))))

(defn write-location [location data]
  (go (try (spit (first (string/split location #"#")) data)
           (catch Exception ex
             {:error ex}))))

(defn decode-json [s]
  (json/parse-string s))

(defn decode-json-ast
  ([location s]
   (decode-json s))
  ([s] (decode-json-ast "" s)))

(defn encode-json [s]
  (json/generate-string s))

(def Err Exception)

(defn ->clj [x] x)
(defn <-clj [x] x)
