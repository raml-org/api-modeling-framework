(ns api-modelling-framework.parser.syntax.yaml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [api-modelling-framework.parser.syntax.yaml :as yaml]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])))


(deftest load-yaml-test
  (async done
         (go (let [result (<! (yaml/parse-file "resources/world-music-api/api.raml"))]
               (is (not (nil? result)))
               (is (-> result
                       (get (keyword "@location"))
                       (string/ends-with? "api.raml")))
               (is (-> result
                       (get (keyword "@fragment"))
                       (string/starts-with? "#%RAML 1.0")))
               (is (-> result
                       (get (keyword "@data"))
                       (get :traits)
                       (get :secured)
                       (get (keyword "@location"))
                       (string/ends-with? "accessToken.raml")))
               (is (-> result
                       (get (keyword "@data"))
                       (get :traits)
                       (get :secured)
                       (get (keyword "@fragment"))
                       (= "#%RAML 1.0 Trait"))))
             (done))))


(deftest load-yaml-2-test
  (async done
         (go (let [result (<! (yaml/parse-file "http://exchange.org/client1/world-music-api/api.raml"
                                               {"cacheDirs" {"http://exchange.org/client1/world-music-api"
                                                             "resources/world-music-api"}}))]
               (is (not (nil? result)))
               (is (-> result
                       (get (keyword "@location"))
                       (string/ends-with? "api.raml")))
               (is (-> result
                       (get (keyword "@fragment"))
                       (string/starts-with? "#%RAML 1.0")))
               (is (-> result
                       (get (keyword "@data"))
                       (get :traits)
                       (get :secured)
                       (get (keyword "@location"))
                       (string/ends-with? "accessToken.raml")))
               (is (-> result
                       (get (keyword "@data"))
                       (get :traits)
                       (get :secured)
                       (get (keyword "@fragment"))
                       (= "#%RAML 1.0 Trait")))
               (is (=
                    "http://exchange.org/client1/world-music-api/songs-library.raml"
                    (-> result
                        (get (keyword "@data"))
                        :uses
                        :Songs
                        (get (keyword "@location"))))))
             (done))))
