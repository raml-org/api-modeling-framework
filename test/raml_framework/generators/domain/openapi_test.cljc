(ns raml-framework.generators.domain.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.generators.domain.openapi :as generator]
            [raml-framework.parser.domain.openapi :as openapi-parser]))

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
