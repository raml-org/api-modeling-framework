(ns raml-framework.generators.domain.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.generators.domain.openapi :as generator]))

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
