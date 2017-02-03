(ns api-modelling-framework.generators.domain.raml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.raml :as generator]
            [api-modelling-framework.parser.domain.raml :as raml-parser]))

(deftest to-raml-APIDocumentation
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
    (is (= {:title "name"
            :description "description"
            :version "1.0"
            :baseUri "http://test.com/path"
            :protocols ["http" "https"]
            :mediaType ["appliaton/json" "application/json"]}
           (generator/to-raml api-documentation {})))))


(deftest to-raml-EndPoint
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
        generated (generator/to-raml api-documentation {})]
    (is (= input generated))))


(deftest to-raml-Operations
  (let [input {(keyword "/users") {:displayName "Users"
                                   :get {:displayName "get method"
                                         :description "get description"
                                         :protocols ["http"]}
                                   :post {:displayName "post method"
                                          :description "post description"
                                          :protocols ["http"]}}}
        api-documentation (raml-parser/parse-ast input
                                                 {:location "file://path/to/resource.raml#"
                                                  :parsed-location "file://path/to/resource.raml#"
                                                  :is-fragment false
                                                  :path "/test"})
        generated (generator/to-raml api-documentation {})]
    (is (= input generated))))

(deftest to-raml-Response
  (let [input {:displayName "get method"
               :description "get description"
               :protocols ["http"]
               :responses {"200" {:description "200 response"}
                           "400" {:description "400 response"}}}
        parsed (raml-parser/parse-ast input {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                             :location "file://path/to/resource.raml#/users"
                                             :path "/users"
                                             :is-fragment false})
        generated (generator/to-raml parsed {})]
    (is (= input generated)))
  (let [input {:displayName "get method"
               :description "get description"
               :protocols ["http"]
               :responses {"200" {:description "200 response"
                                  :body {"application/json" {:type "any"}
                                         "text/plain"       {:type "any"}}}
                           "400" {:description "400 response"
                                  :body {:type "any"}}}}
        parsed (raml-parser/parse-ast input {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                             :location "file://path/to/resource.raml#/users"
                                             :path "/users"
                                             :is-fragment false})
        generated (generator/to-raml parsed {})]
    (is (= input generated))))


(deftest to-raml-operation-with-request
  (let [input {:displayName "get method"
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
        parsed (raml-parser/parse-ast input {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                             :location "file://path/to/resource.raml#/users"
                                             :path "/users"
                                             :method "get"
                                             :is-fragment false})
        generated (generator/to-raml parsed {})]
    (is(= generated input))))
