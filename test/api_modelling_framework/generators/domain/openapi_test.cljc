(ns api-modelling-framework.generators.domain.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.openapi :as generator]
            [api-modelling-framework.parser.domain.openapi :as openapi-parser]
            [api-modelling-framework.parser.domain.raml :as raml-parser]))

(deftest to-openapi-APIDocumentation
  (let [api-documentation (domain/map->ParsedAPIDocumentation {:host "test.com"
                                                               :scheme ["http" "https"]
                                                               :base-path "/path"
                                                               :accepts ["application/json"]
                                                               :content-type ["appliaton/json"]
                                                               :version "1.0"
                                                               :terms-of-service "terms"
                                                               :id "id"
                                                               :name "name"
                                                               :description "description"})]
    (is (= {:host "test.com"
            :scheme ["http" "https"]
            :basePath "/path"
            :produces "appliaton/json"
            :info
            {:title "name"
             :description "description"
             :version "1.0"
             :termsOfService "terms"}
            :consumes "application/json"}
           (generator/to-openapi api-documentation {})))))


(deftest to-openapi-EndPoint
  (let [node {:swagger "2.0"
              :info {:title "title"
                     :description "description"
                     :termsOfService "terms-of-service"
                     :version "2.0"}
              :host "api.test.com"
              :basePath "/test/endpoint"
              :schemes ["http" "https"]
              :consumes ["application/json" "application/xml"]
              :produces ["application/ld+json"]
              :paths {(keyword "/users") {:get {}}
                      (keyword "/users/items") {:get {}}}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                               :parsed-location "file://path/to/resource.raml#"
                                               :is-fragment false})
        generated (generator/to-openapi parsed {})]
    (is (= (->> node :paths keys)
           (->> generated :paths keys)))))

(deftest to-openapi-operations
  (let [node {:get {:operationId "get"
                    :description "get description"
                    :schemes ["https"]
                    :tags ["experimantl" "foo" "bar"]
                    :produces ["application/ld+json"]
                    :consumes ["application/json"]}
              :post {:operationId "post"
                     :description "post description"
                     :schemes ["https"]
                     :tags ["experimantl" "foo" "bar"]
                     :produces ["application/ld+json"]
                     :consumes ["application/json"]}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#/users"
                                               :parsed-location "file://path/to/resource.raml#/users"
                                               :is-fragment false
                                               :path "/Users"})
        generated (generator/to-openapi parsed {})]
    (is (= node generated))))

(deftest to-openapi-response
  (let [node {:operationId "get method"
              :description "get description"
              :schemes ["http"]
              :responses {"200" {:description "200 response"}
                          "400" {:description "400 response"}}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#/users"
                                               :parsed-location "file://path/to/resource.raml#/users"
                                               :is-fragment false
                                               :path "/Users"})
        generated (generator/to-openapi parsed {})]
    (is (= node generated)))
  (let [parsing-context {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                         :location "file://path/to/resource.raml#/users"
                         :path "/users"
                         :is-fragment false}
        node {:displayName "get method"
              :description "get description"
              :protocols ["http"]
              :responses {200 {:description "200 response"
                               :body {"application/json" {:type "string"}
                                      "text/plain"       {:type "string"}}}
                          400 {:description "400 response"
                               :body {:type "string"}}}}
        parsed (raml-parser/parse-ast node parsing-context)
        generated (generator/to-openapi parsed {})]
    (is (= {:operationId "get method",
            :description "get description",
            :x-response-bodies-with-media-types true,
            :schemes ["http"],
            :produces ["application/json" "text/plain"],
            :responses
            {"200--application/json" {:description "200 response"},
             "200--text/plain" {:description "200 response"},
             "400" {:description "400 response"}}}
           generated))))
