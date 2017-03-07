(ns api-modelling-framework.parser.domain.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.parser.domain.jsonld :as jsonld-parser]
            [api-modelling-framework.parser.domain.raml :as raml-parser]
            [api-modelling-framework.parser.domain.openapi :as openapi-parser]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.jsonld :as generator]
            [api-modelling-framework.generators.domain.raml :as raml-genenerator]
            [api-modelling-framework.generators.domain.openapi :as openapi-genenerator]
            [api-modelling-framework.model.document :as document]))

(deftest from-jsonld-APIDocumentation
  (let [data {"@type" [v/http:APIDocumentation]
              "@id"   "file://test.json#/APIDocumentation"
              v/sorg:name [{"@value" "name"}]
              v/sorg:description [{"@value" "description"}]
              v/sorg:version [{"@value" "1.0"}]
              v/http:scheme [{"@value" "http"}]
              v/http:host [{"@value" "domain.com"}]
              v/http:terms-of-service [{"@value" "terms of service"}]
              v/http:accepts [{"@value" "application/jsonld"} {"@value" "text/plain"}]
              v/document:source [{"@id" "file://test.json/#/source-tag"
                                  "@type" v/document:SourceMap
                                  v/document:location [{"@id" "file://test.json/"}]
                                  v/document:tag [{"@id" "file://test.json/#/source-tag/node-tag"
                                                   "@type" [v/document:Tag]
                                                   v/document:tag-id [{"@value" document/node-parsed-tag}]
                                                   v/document:tag-value [{"@value" "root"}]}]}]}
        parsed (jsonld-parser/from-jsonld data)
        expected {document/id "file://test.json#/APIDocumentation"
                  document/name "name"
                  document/description "description"
                  domain/version "1.0"
                  domain/scheme ["http"]
                  domain/host "domain.com"
                  domain/terms-of-service "terms of service"
                  domain/accepts ["application/jsonld" "text/plain"]}
        source (first (document/sources parsed))]
    (doseq [[m expected-value] expected]
      (is (= expected-value (m parsed))))
    (is (= "file://test.json/" (document/source source)))
    (is (= 1 (count (document/tags source))))
    (is (= document/node-parsed-tag (document/tag-id (first (document/tags source)))))))

(deftest from-jsonld-EndPoint
  (let [input {:baseUri "http://test.com"
               :protocols "http"
               :version "1.0"
               (keyword "/users") {:displayName "Users"
                                   (keyword "/items") {:displayName "items"
                                                       (keyword "/prices") {:displayName "prices"}}}}
        api-documentation (raml-parser/parse-ast input
                                                 {:location "file://path/to/resource.raml#"
                                                  :parsed-location "file://path/to/resource.raml#"
                                                  :is-fragment false})
        generated (generator/to-jsonld api-documentation {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= input (raml-genenerator/to-raml parsed {})))))

(deftest from-jsonld-Operation
  (let [input {:displayName "Users"
               :get {:displayName "get method"
                     :description "get description"
                     :protocols ["http"]}
               :post {:displayName "post method"
                      :description "post description"
                      :protocols ["http"]}}
        model-parsed (raml-parser/parse-ast input
                                            {:location "file://path/to/resource.raml#"
                                             :parsed-location "file://path/to/resource.raml#"
                                             :is-fragment false})
        generated (generator/to-jsonld (first model-parsed) {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= input (raml-genenerator/to-raml parsed {}))))
  (let [input {:get {:operationId "get"
                     :description "get description"
                     :schemes ["https"]
                     :tags ["experimantl" "foo" "bar"]
                     :produces ["application/ld+json"]
                     :responses {"default" {:description ""}}
                     :consumes ["application/json"]}
               :post {:operationId "post"
                      :description "post description"
                      :schemes ["https"]
                      :tags ["experimantl" "foo" "bar"]
                      :produces ["application/ld+json"]
                      :responses {"default" {:description ""}}
                      :consumes ["application/json"]}}
        model-parsed (openapi-parser/parse-ast input
                                               {:location "file://path/to/resource.raml#"
                                                :parsed-location "file://path/to/resource.raml#"
                                                :is-fragment false
                                                :path "/Users"})
        generated (generator/to-jsonld model-parsed {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)
        _ (clojure.pprint/pprint parsed)
        output (openapi-genenerator/to-openapi parsed {})]
    (is (= input output))))

(deftest from-jsonld-Operation2
  (let [input {:displayName "Users"
               :get {:displayName "get method"
                     :description "get description"
                     :protocols ["http"]
                     :responses {"200" {:description "200 response"}
                                 "400" {:description "400 response"}}}}
        model-parsed (raml-parser/parse-ast input
                                            {:location "file://path/to/resource.raml#"
                                             :parsed-location "file://path/to/resource.raml#"
                                             :is-fragment false})
        generated (generator/to-jsonld (first model-parsed) {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= input (raml-genenerator/to-raml parsed {}))))
  (let [input {:get {:responses {"200" {:description "200 response"}
                                 "error" {:description "error response"}}}}
        model-parsed (openapi-parser/parse-ast input
                                               {:location "file://path/to/resource.raml#"
                                                :parsed-location "file://path/to/resource.raml#"
                                                :is-fragment false
                                                :path "/Users"})
        generated (generator/to-jsonld model-parsed {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= input (openapi-genenerator/to-openapi parsed {})))))


(deftest from-jsonld-Response
  (let [input {:displayName "get method"
               :description "get description"
               :protocols ["http"]
               :responses {"200" {:description "200 response"
                                  :body {"application/json" {:type "any"}
                                         "text/plain"       {:type "any"}}}
                           "400" {:description "400 response"
                                  :body {:type "any"}}}}
        model-parsed (raml-parser/parse-ast input
                                            {:location "file://path/to/resource.raml#"
                                             :parsed-location "file://path/to/resource.raml#"
                                             :is-fragment false})
        generated (generator/to-jsonld model-parsed {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= input (raml-genenerator/to-raml parsed {})))))


(deftest from-jsonld-operations-with-request
  (let [node {:displayName "get method"
              :description "get description"
              :protocols ["http"]
              :headers {:Zencoder-Api-Key {:type "integer"}}
              :body {:type "string"}
              :queryParameters {:page {:type "integer"
                                       :required true}
                                :per_page {:type "integer"}}
              :responses {"200" {:description "200 response"
                                 :body {"application/json" {:type "string"}
                                        "text/plain"       {:type "string"}}}
                          "400" {:description "400 response"
                                 :body {:type "string"}}}}
        model-parsed (raml-parser/parse-ast node
                                            {:location "file://path/to/resource.raml#"
                                             :parsed-location "file://path/to/resource.raml#"
                                             :is-fragment false
                                             :metho "get"})
        generated (generator/to-jsonld model-parsed {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)]
    (is (= node (raml-genenerator/to-raml parsed {}))))
  (let [input {:get {:operationId "get"
                    :description "get description"
                    :schemes ["https"]
                    :tags ["experimantl" "foo" "bar"]
                    :produces ["application/ld+json"]
                    :consumes ["application/json"]
                    :responses {"default" {:description ""}},
                    :parameters [{:name "api-key"
                                  :in "header"
                                  :type "string"}
                                 {:name "petId"
                                  :in "path"
                                  :required true
                                  :type "string"}
                                 {:name "race"
                                  :in "query"
                                  :type "string"}
                                 {:name "the-body"
                                  :x-media-type "*/*"
                                  :in "body"
                                  :schema {:type "string"}}]}
              :post {:operationId "post"
                     :description "post description"
                     :schemes ["https"]
                     :tags ["experimantl" "foo" "bar"]
                     :responses {"default" {:description ""}}
                     :produces ["application/ld+json"]
                     :consumes ["application/json"]}}
        model-parsed (openapi-parser/parse-ast input
                                               {:location "file://path/to/resource.raml#"
                                                :parsed-location "file://path/to/resource.raml#"
                                                :is-fragment false
                                                :path "/Users"})
        generated (generator/to-jsonld model-parsed {:source-maps? true})
        parsed (jsonld-parser/from-jsonld generated)
        output (openapi-genenerator/to-openapi parsed {})]
    (is (= input output))))

(deftest traits-test
  (let [location "file://path/to/resource.raml#"
        input {:title "Github API"
               :baseUri "http://api.github.com"
               :protocols "http"
               :version "v3"
               :traits {:paged
                        {:queryParameters
                         {:start {:type "number"}}}}
               (keyword "/users") {:displayName "Users"
                                   :get {:description "get description"
                                         :is ["paged"]
                                         :protocols ["http"]
                                         :responses {"200" {:description "200 response"}
                                                     "400" {:description "400 response"}}}}}
        declarations (raml-parser/process-traits input {:location (str location "")
                                                        :parsed-location (str location "/declares")})
        model-parsed (raml-parser/parse-ast input
                                            {:parsed-location location
                                             :location location
                                             :references declarations
                                             :is-fragment false})
        generated (generator/to-jsonld model-parsed {:source-maps? true})

        parsed (jsonld-parser/from-jsonld generated)

        output (raml-genenerator/to-raml parsed {:parsed-location location
                                                 :location location
                                                 :references (vals declarations)
                                                 :is-fragment false})]
    (is (= output input))))
