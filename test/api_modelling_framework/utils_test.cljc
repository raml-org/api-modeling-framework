(ns api-modelling-framework.utils-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.test :refer [deftest is]]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            [api-modelling-framework.utils :as utils]))

(deftest safe-str-test
  (is (= (utils/safe-str "test") "test"))
  (is (= (utils/safe-str :test) "test"))
  (is (= (utils/safe-str (keyword "/test")) "/test"))
  (is (= (utils/safe-str (keyword "application/json")) "application/json")))


(defn cb->chan [f]
  (let [c (chan)]
    (f (fn [e o]
         (go (if (some? e)
               (>! c {:error e})
               (>! c o)))))
    c))

(defn error? [x]
  (if (and (map? x)
           (some? (:error x)))
    (do (prn (:error x))
        true)
    false))
