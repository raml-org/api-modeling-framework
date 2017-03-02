(ns api-modelling-framework.integration-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [api-modelling-framework.core :as core]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.platform :as platform]
            [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])))

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

(deftest integration-test-raml->open-api
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->OpenAPIGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   _ (is (not (error? model)))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   raml-string (<! (cb->chan (partial core/generate-string generator "resources/world-music-api/wip.raml"
                                                      output-model
                                                      {})))
                   _ (is (not (error? raml-string)))
                   output (platform/decode-json raml-string)]
               (is (= ["Album" "Track"] (-> output (get "definitions") keys)))
               (is (some? (-> output
                              (get "definitions")
                              (get "Track")
                              (get "properties")
                              (get "song")
                              (get "$ref"))))
               (is (some? (-> output (get "x-uses") (count))))
               (is (some? (-> output (get "x-traits") (get "secured") (get "$ref"))))
               (is (= ["secured"] (-> output (get "paths") (get "/albums") (get "x-is"))))
               (is (= "#/definitions/Album"
                      (-> output
                          (get "paths")
                          (get "/albums")
                          (get "get")
                          (get "responses")
                          (get "200")
                          (get "schema")
                          (get "items")
                          (get "$ref"))))
               (done)))))

(deftest integration-test-raml->raml
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   _ (is (not (error? model)))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-string (<! (cb->chan (partial core/generate-string generator "resources/world-music-api/wip.raml"
                                                      output-model
                                                      {})))
                   _ (is (not (error? output-string)))
                   output (syntax/<-data (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-string)))]
               (is (= [:Album :Track] (-> output :types keys)))
               (is (= "SongsLib.Song" (-> output :types :Track :properties :song :type)))
               (is (some? (-> output :uses :SongsLib)))
               (is (some? (-> output :traits :secured)))
               (is (= ["secured"] (-> output (get (keyword "/albums")) :is)))
               (is (= "Album" (-> output (get (keyword "/albums")) :get :responses :200 :body :items)))
               (done)))))

(comment
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
                 (core/parse-file open-api-parser "resources/test.json"
                                  (fn [error model]
                                    (is (nil? error))
                                    (is (some? model))
                                    (let [domain-model (core/domain-model model)]
                                      ;;(prn domain-model)
                                      ;;(println "GENEARTING STRING NOW...")
                                      (core/generate-string open-api-generator "file://resources/petstore.json"
                                                            domain-model
                                                            {}
                                                            (fn [error raml-string]
                                                              ;;(println "BACK!")
                                                              ;;(prn error)
                                                              ;;(println "I'M BACK")
                                                              ;;(println raml-string)
                                                              (is true)
                                                              (done))))))))))

  (deftest integration-test-6
    (async done
           (go (let [parser (core/->RAMLParser)
                     generator (core/->RAMLGenerator)]
                 (core/parse-file parser "resources/world-music-api/wip.raml"
                                  (fn [error model]
                                    (is (nil? error))
                                    (is (some? model))
                                    (let [output-model (core/document-model model)]
                                      ;;(println "ALL DECLARED TYPES")
                                      ;;(doseq [dec (:declares output-model)]
                                      ;; (println (:id dec)))
                                      ;;(prn output-model)
                                      ;;(println "GENEARTING STRING NOW...")
                                      (core/generate-string generator "resources/world-music-api/wip.raml"
                                                            output-model
                                                            {}
                                                            (fn [error raml-string]
                                                              ;;(println "BACK!")
                                                              ;;(prn error)
                                                              ;;(println "I'M BACK")
                                                              ;;(println raml-string)
                                                              (is true)
                                                              (done))))))))))

  (deftest integration-test-7
    (async done
           (go (let [parser (core/->RAMLParser)
                     generator (core/->APIModelGenerator)]
                 (core/parse-file parser "resources/world-music-api/real_api.raml"
                                  (fn [error model]
                                    (is (nil? error))
                                    (is (some? model))
                                    (let [output-model (core/document-model model)]
                                      ;;(prn output-model)
                                      ;;(println "GENEARTING STRING NOW...")
                                      (core/generate-string generator "resources/world-music-api/wip.raml"
                                                            output-model
                                                            {}
                                                            (fn [error raml-string]
                                                              (println "BACK!")
                                                              (prn error)
                                                              (println "I'M BACK")
                                                              (println raml-string)
                                                              (is true)
                                                              (done)))))))))))
