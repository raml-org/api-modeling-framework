(ns api-modelling-framework.integration-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [api-modelling-framework.core :as core]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])))


(deftest integration-test-1
  (async done
         (go (let [raml-parser (core/->RAMLParser)
                   openapi-generator (core/->OpenAPIGenerator)
                   openapi-parser (core/->OpenAPIParser)
                   raml-generator (core/->RAMLGenerator)]
               (core/parse-file raml-parser "resources/world-music-api/api.raml"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  ;;(clojure.pprint/pprint (core/document-model model))
                                  (core/generate-string openapi-generator "file://test/world-music.raml"
                                                        (core/document-model model)
                                                        {:inline-fragments true}
                                                        (fn [error openapi-string]
                                                          (is (nil? error))
                                                          (is (some? openapi-string))
                                                          ;;(println "OPENAPI")
                                                          ;;(println openapi-string)
                                                          (core/parse-string openapi-parser "file://test/world-music.raml"
                                                                             openapi-string
                                                                             (fn [error parsed-model]
                                                                               (is (nil? error))
                                                                               (is (some? parsed-model))
                                                                               (core/generate-string raml-generator "file://test/world-music.raml"
                                                                                                     (core/document-model parsed-model)
                                                                                                     {:inline-fragments true}
                                                                                                     (fn [error raml-string]
                                                                                                       (is (nil? error))
                                                                                                       (is (some? raml-string))
                                                                                                       (println raml-string)
                                                                                                       (is true)
                                                                                                       (done)))))))))))))


(deftest integration-test-2
  (async done
         (go (let [raml-parser (core/->RAMLParser)
                   openapi-generator (core/->OpenAPIGenerator)
                   openapi-parser (core/->OpenAPIParser)
                   raml-generator (core/->RAMLGenerator)]
               (core/parse-file raml-parser "resources/world-music-api/api.raml"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  (core/generate-string raml-generator "file://test/world-music.raml"
                                                        (core/document-model model)
                                                        {}
                                                        (fn [error raml-string]
                                                          (println raml-string)
                                                          (is true)
                                                          (done)))))))))


(deftest integration-test-3
  (async done
         (go (let [raml-parser (core/->RAMLParser)
                   openapi-generator (core/->OpenAPIGenerator)
                   openapi-parser (core/->OpenAPIParser)
                   raml-generator (core/->RAMLGenerator)]
               (core/parse-file raml-parser "resources/world-music-api/api.raml"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  (let [domain-model (core/domain-model model)]
                                    ;;(prn domain-model)
                                    ;;(println "GENEARTING STRING NOW...")
                                    (core/generate-string raml-generator "file://test/world-music.raml"
                                                          domain-model
                                                          {}
                                                          (fn [error raml-string]
                                                            ;;(println "BACK!")
                                                            ;;(prn error)
                                                            ;;(println "I'M BACK")
                                                            ;;(println raml-string)
                                                            (done))))))))))


(deftest integration-test-4
  (async done
         (go (let [raml-parser (core/->RAMLParser)
                   api-model-generator (core/->APIModelGenerator)]
               (core/parse-file raml-parser "resources/world-music-api/api.raml"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  (let [domain-model (core/domain-model model)]
                                    ;;(prn domain-model)
                                    ;;(println "GENEARTING STRING NOW...")
                                    (core/generate-string api-model-generator "file://test/world-music.raml"
                                                          domain-model
                                                          {}
                                                          (fn [error raml-string]
                                                            ;;(println "BACK!")
                                                            ;;(prn error)
                                                            ;;(println "I'M BACK")
                                                            ;;(println raml-string)
                                                            (is true)
                                                            (done))))))))))

(deftest integration-test-4b
  (async done
         (go (let [raml-parser (core/->RAMLParser)
                   api-model-generator (core/->APIModelGenerator)]
               (core/parse-file raml-parser "resources/world-music-api/api.raml"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  (let [document-model (core/document-model model)]
                                    ;;(prn document-model)
                                    ;;(println "GENEARTING STRING NOW...")
                                    (core/generate-string api-model-generator "file://test/world-music.raml"
                                                          document-model
                                                          {}
                                                          (fn [error raml-string]
                                                            ;;(println "BACK!")
                                                            ;;(prn error)
                                                            ;;(println "I'M BACK")
                                                            ;;(println raml-string)
                                                            (is true)
                                                            (done))))))))))

(deftest integration-test-5
  (async done
         (go (let [open-api-parser (core/->OpenAPIParser)
                   open-api-generator (core/->OpenAPIGenerator)]
               (core/parse-file open-api-parser "resources/petstore.json"
                                (fn [error model]
                                  (is (nil? error))
                                  (is (some? model))
                                  (let [domain-model (core/domain-model model)]
                                    (prn domain-model)
                                    (println "GENEARTING STRING NOW...")
                                    (core/generate-string open-api-generator "file://resources/petstore.json"
                                                          domain-model
                                                          {}
                                                          (fn [error raml-string]
                                                            (println "BACK!")
                                                            (prn error)
                                                            (println "I'M BACK")
                                                            (println raml-string)
                                                            (is true)
                                                            (done))))))))))
