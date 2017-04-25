(ns api-modeling-framework.parser.domain.common-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modeling-framework.parser.domain.common :as common]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.utils :as utils]))


(deftest wrapped-ast-token?-test
  (is (common/wrapped-ast-token? {:amf-lexical-token 1}))
  (is (not (common/wrapped-ast-token? 1)))
  (is (not (common/wrapped-ast-token? {:a 1})))
  (is (not (common/wrapped-ast-token? nil)))
  (is (not (common/wrapped-ast-token? [:a :b]))))

(deftest with-ast-parsing-test
  (let [node (with-meta {:a 1} {:location true})
        result (common/with-ast-parsing node
                 (fn [{:keys [a]}] {:res (inc a)}))]
    (is (= {:res 2 :lexical {:location true}} result)))
  (let [node (with-meta {:amf-lexical-token 1} {:location true})
        result (common/with-ast-parsing node
                 (fn [a] {:res (inc a)}))]
    (is (= {:res 2 :lexical {:location true}} result)))
  (let [node {:a 1}
        result (common/with-ast-parsing node
                 (fn [{:keys [a]}] {:res (inc a)}))]
    (is (= {:res 2} result))))

(deftest ast-value-test
  (is (= 1 (common/ast-value {:amf-lexical-token 1})))
  (is (= 1 (common/ast-value 1))))

(deftest ast-get-test
  (is (= 1 (common/ast-get {:a 1} :a)))
  (is (= 1 (common/ast-get {:a {:amf-lexical-token 1}} :a)))
  (is (= 2 (common/ast-get {:a {:amf-lexical-token 1}} :b 2)))
  (is (= 2 (common/ast-get {:a 1} :b 2))))

(deftest ast-assoc-test
  (is (= {:a 1} (common/ast-assoc {} :a 1)))
  (is (= {:a 1} (common/ast-assoc {} :a {:amf-lexical-token 1}))))
