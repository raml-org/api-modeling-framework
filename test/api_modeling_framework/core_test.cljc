(ns api-modeling-framework.core-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require #?(:clj [clojure.test :refer :all])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [api-modeling-framework.platform :refer [async]])
            #?(:clj [clojure.test :refer [deftest is]])
            [api-modeling-framework.utils-test :refer [cb->chan error?]]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            [api-modeling-framework.core :as core]))

(deftest lexical-info-raml-test
  (async done
         (go
           (let [parser (core/->RAMLParser)
                 model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                 id #?(:cljs "file://resources/world-music-api/wip.raml#/api-documentation/end-points/%2Falbums/post/body"
                       :clj "resources/world-music-api/wip.raml#/api-documentation/end-points/%2Falbums/post/body")
                 info (core/lexical-info-for-unit model id)]
             (is (some? info))
             (done)))))


#?(:cljs
   (deftest lexical-info-raml-test
     (async done
            (go
              (let [parser (core/->OpenAPIParser)
                    model (<! (cb->chan (partial core/parse-file parser "resources/petstore.json")))
                    id "file://./resources/petstore.json#/paths/%2Fpets/%2Fpets/get/get"
                    info(core/lexical-info-for-unit model id)]
                (is (some? info))

                (done))))))
