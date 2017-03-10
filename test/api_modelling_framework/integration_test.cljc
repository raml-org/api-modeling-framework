(ns api-modelling-framework.integration-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [api-modelling-framework.core :as core]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.platform :as platform]
            [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
            [api-modelling-framework.utils :as utils]
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

(deftest integration-test-raml->domain->raml
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   _ (is (not (error? model)))
                   output-model (core/domain-model model)
                   _ (is (not (error? output-model)))
                   output-string (<! (cb->chan (partial core/generate-string generator "resources/world-music-api/wip.raml"
                                                      output-model
                                                      {})))
                   _ (is (not (error? output-string)))
                   output (syntax/<-data (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-string)))
                   types (->> output
                              (filter (fn [[k v]] (string/starts-with? (utils/safe-str k) "/")))
                              (map last)
                              (map :get)
                              (map :responses)
                              (map :200)
                              (map :body))]
               (prn types)
               (doseq [type types]
                 (is (or (= (:type type) "array")
                         (= (:type type) "object")))
                 (if (= "array" (:type type))
                   (is (> (count (:items type)) 0))
                   (is (> (count (:properties type)) 0))))
               (done)))))

(deftest integration-test-raml->wm
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/other-examples/world-music-api/api.raml")))
                   document-model (core/document-model model)
                   api-documentation (document/encodes document-model)
                   paths (->> (domain/endpoints api-documentation)
                              (mapv (fn [endpoint] [(domain/path endpoint) endpoint]))
                              (into {}))
                   api-resource (get paths "/api")
                   song-resource (get paths "/songs/{songId}")]
               (is (= "World Music API" (document/name api-documentation)))
               (is (= "This is an example of a music API." (document/description api-documentation)))
               (is (= "/{version}" (domain/base-path api-documentation)))
               (is (= "v1" (domain/version api-documentation)))
               (is (= 4 (count (domain/endpoints api-documentation))))
               (is (= 2 (count (domain/supported-operations api-resource))))
               (is (= "get" (->>
                             (domain/supported-operations api-resource)
                             first
                             (domain/method))))
               (is (= "post" (->>
                             (domain/supported-operations api-resource)
                             last
                             (domain/method))))
               (is (= 1 (->>
                         (domain/supported-operations api-resource)
                         last
                         (domain/request)
                         (domain/payloads)
                         count)))
               (is (= "application/json" (->>
                                          (domain/supported-operations api-resource)
                                          last
                                          (domain/request)
                                          (domain/payloads)
                                          first
                                          domain/media-type
                                          utils/safe-str)))
               (is (= "/declares/types/RamlDataType/type/shape"
                      (let [type (->>
                                  (domain/supported-operations api-resource)
                                  last
                                  (domain/request)
                                  (domain/payloads)
                                  first
                                  (domain/schema)
                                  (domain/shape))]
                        (last (string/split (first (get type (v/shapes-ns "inherits"))) #"#")))))
               (is (= "application/xml"
                      (->> song-resource
                           (domain/supported-operations)
                           first
                           (domain/responses)
                           first
                           (domain/payloads)
                           last
                           (domain/media-type)
                           (utils/safe-str))))
               (is (->> song-resource
                             (domain/supported-operations)
                             first
                             (domain/responses)
                             first
                             (domain/payloads)
                             last
                             (domain/schema)
                             (domain/shape)
                             some?))
               (done)))))

(defn test-raml-document-level [generator-raml output-document-model]
  (go (let [output-document-raml (<! (cb->chan (partial core/generate-string generator-raml "resources/world-music-api/wip.raml"
                                                        output-document-model
                                                        {})))
            raw (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-document-raml))
            ;; JS / JAVA parsers behave in a slightly different way, that's the reason for the or
            parsed-document-raml-output (or (syntax/<-data raw)
                                            (:data (get raw (keyword "resources/world-music-api/wip.raml"))))]
        (is (string? (-> parsed-document-raml-output
                         (get (keyword "/songs"))
                         (get (keyword "/{songId}"))
                         :get
                         :responses
                         :200
                         :body
                         (get (keyword "application/xml"))))))))

(defn test-raml-domain-level [generator-raml output-domain-model]
  (go (let [output-domain-raml (<! (cb->chan (partial core/generate-string generator-raml "resources/world-music-api/wip.raml"
                                                      output-domain-model
                                                      {})))
            raw (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-domain-raml))
            ;; JS / JAVA parsers behave in a slightly different way, that's the reason for the or
            parsed-domain-raml-output (or
                                       ;; java
                                       (syntax/<-data raw)
                                       ;; js
                                       (:data (get raw (keyword "resources/world-music-api/wip.raml"))))]
        (is (string? (-> parsed-domain-raml-output
                         (get (keyword "/{version}/songs/{songId}"))
                         :get
                         :responses
                         :200
                         :body
                         (get (keyword "application/xml"))))))))

(defn test-openapi-document-level [generator-openapi output-document-model]
  (go (let [output-document-openapi (<! (cb->chan (partial core/generate-string generator-openapi "resources/world-music-api/wip.json"
                                                        output-document-model
                                                        {})))
            parsed-document-openapi-output (platform/decode-json output-document-openapi)
            fragment-location (-> parsed-document-openapi-output
                                  (get "paths")
                                  (get "/songs/{songId}")
                                  (get "get")
                                  (get "responses")
                                  (get "200")
                                  (get "x-responses")
                                  first
                                  (get "schema")
                                  (get "$ref"))]
        (is (string? fragment-location))
        (is (string/ends-with? fragment-location ".xsd")))))

(defn test-openapi-domain-level [generator-openapi output-domain-model]
  (go (let [output-domain-openapi (<! (cb->chan (partial core/generate-string generator-openapi "resources/world-music-api/wip.json"
                                                      output-domain-model
                                                      {})))
            parsed-domain-openapi-output (platform/decode-json output-domain-openapi)]
        (is (-> parsed-domain-openapi-output
                (get "paths")
                (get "/{version}/songs/{songId}")
                (get "get")
                (get "responses")
                (get "200")
                (get "x-responses")
                first
                (get "schema")
                (get "value")
                string?)))))

(deftest integration-test-raml-wm->domain
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator-openapi (core/->OpenAPIGenerator)
                   generator-raml (core/->RAMLGenerator)
                   generator-jsonld (core/->APIModelGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/other-examples/world-music-api/api.raml")))
                   output-model (core/domain-model model)
                   output-document-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-jsonld (<! (cb->chan (partial core/generate-string generator-jsonld "resources/world-music-api/wip.raml"
                                                        output-model
                                                        {})))
                   _ (is (not (error? output-jsonld)))]
               (<! (test-raml-document-level generator-raml output-document-model))
               (<! (test-raml-domain-level generator-raml output-model))
               (<! (test-openapi-document-level generator-openapi output-document-model))
               (<! (test-openapi-domain-level generator-openapi output-model))
               (done)))))

(deftest integration-test-openapi-ps->domain
  (async done
         (go (let [parser (core/->OpenAPIParser)
                   generator-openapi (core/->OpenAPIGenerator)
                   generator-raml (core/->RAMLGenerator)
                   generator-jsonld (core/->APIModelGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "file://resources/petstore.json")))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-openapi (<! (cb->chan (partial core/generate-string generator-openapi "resources/pestore.json"
                                                         output-model
                                                         {})))
                   output-raml (<! (cb->chan (partial core/generate-string generator-raml "resources/petstore.raml"
                                                        output-model
                                                        {})))
                   output-jsonld (<! (cb->chan (partial core/generate-string generator-jsonld "resources/petstore.jsonld"
                                                        output-model
                                                        {})))]
               ;; @todo ADD ASSERTIONS HERE
               ;;(println output-raml)
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
