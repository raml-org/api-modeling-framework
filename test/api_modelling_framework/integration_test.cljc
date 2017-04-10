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
            [api-modelling-framework.utils-test :refer [cb->chan error?]]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])))


(deftest integration-test-raml->open-api
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->OpenAPIGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   _ (is (not (error? model)))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   open-api-string (<! (cb->chan (partial core/generate-string generator "resources/world-music-api/wip.raml"
                                                      output-model
                                                      {})))
                   _ (is (not (error? open-api-string)))
                   output (platform/decode-json open-api-string)]
               (is (= ["Album" "Track"] (-> output (get "definitions") keys)))
               (is (some? (-> output
                              (get "definitions")
                              (get "Track")
                              (get "properties")
                              (get "song")
                              (get "$ref"))))
               (is (some? (-> output (get "x-uses") (count))))
               (is (some? (-> output (get "x-traits") (get "secured") (get "$ref"))))
               (is (= [{"$ref" "#/x-traits/secured"}] (-> output (get "paths") (get "/albums") (get "x-is"))))
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
               (is (= 2 (-> output (get "x-annotationTypes") count)))
               (is (= ["name"] (-> output (get "x-annotationTypes") (get "behaviour") (get "properties") keys)))
               (is (= "string" (-> output (get "x-annotationTypes") (get "test") (get "type"))))
               (is (= "albumsTest" (-> output (get "paths") (get "/albums") (get "post") (get "x-test"))))
               (is (= "safe" (-> output (get "paths") (get "/albums") (get "get") (get "responses") (get "200") (get "x-behaviour") (get "name"))))
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
               (is (= "SongsLib.Song" (-> output :types :Track :properties :song)))
               (is (some? (-> output :uses :SongsLib)))
               (is (some? (-> output :traits :secured)))
               (is (= ["secured"] (-> output (get (keyword "/albums")) :is)))
               (is (= "Album" (-> output (get (keyword "/albums")) :get :responses :200 :body :items)))
               (is (= {:behaviour {:displayName "behaviour"
                                   :properties {:name "string"}},
                       :test {:displayName "test"
                              :type "string"}}
                      (:annotationTypes output)))
               (is (= "albumsTest" (-> output (get (keyword "/albums")) :post (get (keyword "(test)")))))
               (is (= "safe" (-> output (get (keyword "/albums")) :get :responses :200 (get (keyword "(behaviour)")) :name)))
               (done)))))

(deftest integration-test-raml->api-model
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->APIModelGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   _ (is (not (error? model)))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-string (<! (cb->chan (partial core/generate-string generator "resources/world-music-api/wip.raml"
                                                      output-model
                                                      {})))
                   _ (is (not (error? output-string)))
                   output (platform/decode-json output-string)]
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
               (doseq [type types]
                 (is (or (= (:type type) "array")
                         (some? (:properties type))))
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
               (is (= "/definitions/RamlDataType"
                      (let [type (->>
                                  (domain/supported-operations api-resource)
                                  last
                                  (domain/request)
                                  (domain/payloads)
                                  first
                                  (domain/schema)
                                  (domain/shape))]
                        (last (string/split
                               ;; id of the inherited type
                               (get (first (get type (v/shapes-ns "inherits"))) "@id")
                               #"#")))))
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
                                  (get "x-response-payloads")
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
                (get "x-response-payloads")
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
                   _ (is (not (error? output-jsonld)))
                   ]
               (is (= {:start-line 1,
                       :start-column 0,
                       :start-index 11,
                       :end-line 196,
                       :end-column 0,
                       :end-index 5040}
                      (->> output-document-model
                           :encodes
                           :lexical)))
               (is (some? (:raw output-document-model)))
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
                   yaml-data (syntax/<-data (<! (yaml-parser/parse-string "resources/petstore.raml" output-raml)))
                   output-jsonld (<! (cb->chan (partial core/generate-string generator-jsonld "resources/petstore.jsonld"
                                                        output-model
                                                        {})))]
               (is (some? (:raw output-model)))

               (is (-> yaml-data (get (keyword "/pets")) some?))
               (is (-> yaml-data (get (keyword "/pets/{petId}")) some?))
               (done)))))

(deftest integration-test-update-raml
  (async done
         (go (let [parser (core/->RAMLParser)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   raw (core/raw model)
                   updated-raw (string/replace raw "(WIP)" "")
                   _ (is (not (error? model)))
                   updated (<! (cb->chan (partial core/update-reference-model model
                                                  (core/location model)
                                                  "raml"
                                                  updated-raw)))]
               (is (= "World Music API"
                      (-> updated
                          core/document-model
                          document/encodes
                          document/name)))
               (done)))))

(deftest integration-test-cache-dirs
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->OpenAPIGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/world-music-api/wip.raml")))
                   raw (core/raw model)
                   updated-raw (string/replace raw "(WIP)" "")
                   _ (is (not (error? model)))
                   updated (<! (cb->chan (partial core/update-reference-model model
                                                  (core/location model)
                                                  "raml"
                                                  updated-raw)))]
               (is (= "World Music API"
                      (-> updated
                          core/document-model
                          document/encodes
                          document/name)))
               (done)))))

(deftest integration-test-jsonld-generator-parser
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   jsonld-generator (core/->APIModelGenerator)
                   jsonld-parser (core/->APIModelParser)
                   model (<! (cb->chan (partial core/parse-file parser "resources/ramlapitest1/api.raml")))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-jsonld (<! (cb->chan (partial core/generate-string jsonld-generator "resources/api.raml"
                                                        output-model
                                                        {})))
                   read-model (<! (cb->chan (partial core/parse-string jsonld-parser "resources/api.raml" output-jsonld)))
                   output-yaml(<! (cb->chan (partial core/generate-string generator "resources/api.raml"
                                                     (core/document-model read-model)
                                                     {})))
                   yaml-data (syntax/<-data (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-yaml)))]
               (is (some? (-> yaml-data :types :User)))
               (is (= (-> yaml-data (get (keyword "/users")) :get :responses :200 :body) "User"))
               (done)))))

(deftest integration-test-jsonld-generator-parser-source-maps
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   jsonld-generator (core/->APIModelGenerator)
                   jsonld-parser (core/->APIModelParser)
                   model (<! (cb->chan (partial core/parse-file parser "resources/ramlapitest1/api.raml")))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-jsonld (<! (cb->chan (partial core/generate-string jsonld-generator "resources/api.raml"
                                                        output-model
                                                        {:source-maps? true})))
                   read-model (<! (cb->chan (partial core/parse-string jsonld-parser "resources/api.raml" output-jsonld)))
                   output-yaml(<! (cb->chan (partial core/generate-string generator "resources/api.raml"
                                                     (core/document-model read-model)
                                                     {})))
                   yaml-data (syntax/<-data (<! (yaml-parser/parse-string "resources/world-music-api/wip.raml" output-yaml)))]
               (is (some? (-> yaml-data :types :User)))
               (is (= (-> yaml-data (get (keyword "/users")) :get :responses :200 :body) "User"))
               (done)))))

(deftest integration-test-raml-fragment-libraries
  (async done
         (go (let [parser (core/->RAMLParser)
                   generator (core/->RAMLGenerator)
                   jsonld-generator (core/->APIModelGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "resources/tck/raml-1.0/Fragments/test001/fragment.raml")))
                   _ (is (not (error? model)))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-yaml(<! (cb->chan (partial core/generate-string generator "resources/api.raml"
                                                     output-model
                                                     {})))
                   yaml-data (syntax/<-data (<! (yaml-parser/parse-string "resources/tck/raml-1.0/Fragments/test001/fragment.raml" output-yaml)))
                   output-jsonld (<! (cb->chan (partial core/generate-string jsonld-generator "resources/api.raml"
                                                        output-model
                                                        {:source-maps? false})))
                   output (platform/decode-json output-jsonld)]
               (is (= 2 (-> yaml-data
                            :properties
                            count)))
               (is (some? (-> yaml-data :uses :lib)))
               ;;(clojure.pprint/pprint yaml-data)
               ;;(clojure.pprint/pprint output)
               (done)))))

(comment

  (deftest integration-test-raml->open-api
    (async done
           (go (let [parser (core/->RAMLParser)
                     generator (core/->OpenAPIGenerator)
                     model (<! (cb->chan (partial core/parse-file parser "/Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/mobile-order-api/api.raml")))
                     _ (is (not (error? model)))
                     output-model (core/domain-model model)
                     _ (is (not (error? output-model)))
                     open-api-string (<! (cb->chan (partial core/generate-string generator "/Users/antoniogarrote/Development/tmp/ramlapitest1/api.json"
                                                            output-model
                                                            {})))
                     _ (is (not (error? open-api-string)))
                     output (platform/decode-json open-api-string)]
                 (clojure.pprint/pprint output)
                 (done)))))

  (deftest integration-test-openapi-ps->domain
  (async done
         (go (let [parser (core/->OpenAPIParser)
                   generator-openapi (core/->OpenAPIGenerator)
                   generator-raml (core/->RAMLGenerator)
                   generator-jsonld (core/->APIModelGenerator)
                   model (<! (cb->chan (partial core/parse-file parser "file://resources/uber.json")))
                   output-model (core/document-model model)
                   _ (is (not (error? output-model)))
                   output-openapi (<! (cb->chan (partial core/generate-string generator-openapi "resources/uber.json"
                                                         output-model
                                                         {})))
                   output-raml (<! (cb->chan (partial core/generate-string generator-raml "resources/uber.raml"
                                                        output-model
                                                        {})))
                   yaml-data (syntax/<-data (<! (yaml-parser/parse-string "resources/petstore.raml" output-raml)))
                   output-jsonld (<! (cb->chan (partial core/generate-string generator-jsonld "resources/uber.jsonld"
                                                        output-model
                                                        {})))]
               (clojure.pprint/pprint yaml-data)
               ;;(is (some? (:raw output-model)))
               ;;
               ;;(is (-> yaml-data (get (keyword "/pets")) some?))
               ;;(is (-> yaml-data (get (keyword "/pets/{petId}")) some?))
               (done)))))



  )
