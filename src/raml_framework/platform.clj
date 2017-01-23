(ns raml-framework.platform
  (:require [clojure.core.async :refer [go >! <! <!! thread chan]]))

(defmacro async [s body]
  `(let [~s (fn [] true)
         res# ~body
         finalres# (<!! res#)]
     (when (some? (:err finalres#))
       (throw (Exception. (str (:err finalres#)))))
     (when (instance? Throwable finalres#)
       (throw finalres#))))
