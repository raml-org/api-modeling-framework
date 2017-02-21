(ns api-modelling-framework.utils-test
  (:require [clojure.test :refer :all]
            [api-modelling-framework.utils :as utils]))

(deftest safe-str-test
  (is (= (utils/safe-str "test") "test"))
  (is (= (utils/safe-str :test) "test"))
  (is (= (utils/safe-str (keyword "/test")) "/test"))
  (is (= (utils/safe-str (keyword "application/json")) "application/json")))
