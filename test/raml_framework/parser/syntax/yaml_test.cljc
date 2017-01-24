(ns raml-framework.parser.syntax.yaml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [raml-framework.parser.syntax.yaml :as yaml]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [raml-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])))


(deftest load-yaml-test
  (async done
         (go (let [result (<! (yaml/parse-yaml "resources/world-music-api/api.raml"))]
               (is (not (nil? result)))
               (is (-> result
                       (get (keyword "@location"))
                       (string/ends-with? "api.raml")))
               (is (-> result
                       (get (keyword "@fragment"))
                       (string/starts-with? "#%RAML 1.0")))
               (is (-> result
                       (get :traits)
                       (get :secured)
                       (get (keyword "@location"))
                       (string/ends-with? "accessToken.raml")))
               (is (-> result
                       (get :traits)
                       (get :secured)
                       (get (keyword "@fragment"))
                       (= "#%RAML 1.0 Trait"))))
             (done))))
