(ns api-modelling-framework.generators.domain.raml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.raml :as generator]
            [api-modelling-framework.generators.domain.common :as common]
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
            :baseUri "test.com/path"
            :protocols ["http" "https"]
            :mediaType ["appliaton/json" "application/json"]}
           (generator/to-raml api-documentation {})))))


(deftest to-raml-EndPoint
  (let [input {:baseUri "test.com"
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
                                  :body {"application/json" "any"
                                         "text/plain"       "any"}}
                           "400" {:description "400 response"
                                  :body "any"}}}
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
               :headers {:Zencoder-Api-Key "integer"}
               :body "string"
               :queryParameters {:page "integer"
                                 :per_page "integer"}
               :responses {"200" {:description "200 response"
                                  :body {"application/json" "string"
                                         "text/plain"       "string"}}
                           "400" {:description "400 response"
                                  :body "string"}}}
        parsed (raml-parser/parse-ast input {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                             :location "file://path/to/resource.raml#/users"
                                             :path "/users"
                                             :method "get"
                                             :is-fragment false})
        generated (generator/to-raml parsed {})]
    (is(= generated input))))


(deftest to-raml-traits
  (let [location "file://path/to/resource.raml#"
        input {:title "Github API"
               :baseUri "api.github.com"
               :protocols "http"
               :version "v3"
               :traits {:paged
                        {:queryParameters
                         {:start "number"}}}
               (keyword "/users") {:displayName "Users"
                                   :get {:description "get description"
                                         :is ["paged"]
                                         :protocols ["http"]
                                         :responses {"200" {:description "200 response"}
                                                     "400" {:description "400 response"}}}}}
        declarations (raml-parser/process-traits input {:location (str location "")
                                                        :parsed-location (str location "/declares")})
        parsed (raml-parser/parse-ast input {:parsed-location location
                                             :location location
                                             :references declarations
                                             :is-fragment false})
        generated (generator/to-raml parsed {:references (vals declarations)})]
    (is (= input generated))))


(deftest to-raml-body
  (let [location "file://path/to/file.raml#"
        input {:post {:body {"application/json" {:properties {:name "string"}}}}}
        parsed (raml-parser/parse-ast input {:location location
                                             :parsed-location location})
        generated (generator/to-raml (first parsed) {})]
    (is (= input generated))))

(deftest to-raml-body-2
  (let [location "file://path/to/file.raml#"
        input {:post {:body {:properties {:name "string"}}}}
        parsed (raml-parser/parse-ast input {:location location
                                             :parsed-location location})
        generated (generator/to-raml (first parsed) {})]
    (is (= input generated))))

(deftest to-raml-body-3
  (let [location "file://path/to/file.raml#"
        input {:post {:body {"application/json" {:properties {:name "string"}}
                             "application/json-ld" {:properties {:name2 "string"}}}}}
        parsed (raml-parser/parse-ast input {:location location
                                             :parsed-location location})
        generated (generator/to-raml (first parsed) {})]
    (is (= input generated))))


(deftest to-raml-Annotations
  (let [base-uri "file://path/to/resource.raml"
        location "file://path/to/resource.raml#"
        input {:annotationTypes {:testHarness {:type "string"
                                               :displayName "test harness"
                                               :allowedTargets ["API"]}}
               (keyword "/users") {:displayName "Users"
                                   "(testHarness)" "usersTest"
                                   :get {:displayName "get method"
                                         :description "get description"
                                         :protocols ["http"]}}}
        annotations (raml-parser/process-annotations input {:base-uri base-uri
                                                            :location location
                                                            :parsed-location (str location "/annotations")})
        parsed (raml-parser/parse-ast input
                                      {:location location
                                       :parsed-location "file://path/to/resource.raml#"
                                       :is-fragment false
                                       :annotations annotations
                                       :path "/test"})
        generated-annotations (common/model->annotationTypes (vals annotations) {} generator/to-raml!)
        generated (generator/to-raml parsed {})]
    (is (= input (assoc generated
                        :annotationTypes generated-annotations)))))
