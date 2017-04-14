(ns api-modeling-framework.generators.document.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.parser.document.openapi :as parser]
            [api-modeling-framework.generators.document.openapi :as generator]))


(deftest generate-fragments
  (let [location "file://path/to/resource.raml"
        input {(keyword "@location") location
               (keyword "@fragment") "OpenAPI"
               (keyword "@data") {:swagger "2.0"
                                  :info {:title "title"
                                         :description "description"
                                         :termsOfService "terms-of-service"
                                         :version "2.0"}
                                  :host "api.test.com"
                                  :basePath "/test/endpoint"
                                  :schemes ["http" "https"]
                                  :consumes ["application/json" "application/xml"]
                                  :produces "application/ld+json"
                                  :paths {(keyword "/users") {:get {(keyword "@location") "file://path/to/method.json"
                                                                    (keyword "@data") {:operationId "get"
                                                                                       :description "get description"
                                                                                       :schemes ["https"]
                                                                                       :tags ["experimantl" "foo" "bar"]
                                                                                       :produces ["application/ld+json"]
                                                                                       :consumes ["application/json"]
                                                                                       :responses {"default" {:description ""}}
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
                                                                                                     :in "body"
                                                                                                     :schema {:type "string"}}]}}}}}}
        parsed (parser/parse-ast input {})
        generated (generator/to-openapi parsed {})]
    (is (= generated input))))

(deftest generate-types
  (let [location "file://path/to/resource.json"
        input {(keyword "@location") location
               (keyword "@fragment") "OpenAPI"
               (keyword "@data") {:swagger "2.0"
                                  :info {:title "title"
                                         :description "description"
                                         :termsOfService "terms-of-service"
                                         :version "2.0"}
                                  :definitions {:File {:type "object"
                                                       :properties {:name {:type "string"}}}}
                                  :host "api.test.com"
                                  :basePath "/test/endpoint"
                                  :schemes ["http" "https"]
                                  :consumes ["application/json" "application/xml"]
                                  :produces "application/ld+json"
                                  :paths {(keyword "/files") {:get {:operationId "get"
                                                                    :responses {"default" {:description "the reponse"
                                                                                           :schema {(keyword "$ref") "#/definitions/File"}}}}}}}}
        parsed (parser/parse-ast input {})
        generated (generator/to-openapi parsed {})]
    (is (= input  generated))))
