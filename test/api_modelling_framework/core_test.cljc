(ns api-modelling-framework.core-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require #?(:clj [clojure.test :refer :all])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.test :refer [deftest is]])
            [api-modelling-framework.utils-test :refer [cb->chan error?]]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            [api-modelling-framework.core :as core]))

(deftest lexical-info-test
  (async done
         (go
           (let [parser (core/->RAMLParser)
                 generator (core/->OpenAPIGenerator)
                 model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                 id #?(:cljs "file://resources/world-music-api/wip.raml#/api-documentation/end-points/0/post/body"
                       :clj "resources/world-music-api/wip.raml#/api-documentation/end-points/0/post/body")
                 info(core/lexical-info-for-unit model "raml" id)]
             (is (some? info))
             (done)))))
