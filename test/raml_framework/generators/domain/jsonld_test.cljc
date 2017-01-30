(ns raml-framework.generators.domain.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.generators.domain.jsonld :as generator]))

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
    (is (= {"@id" "id",
            "http://raml.org/vocabularies/http#scheme" '({"@value" "http"} {"@value" "https"}),
            "http://schema.org/version" '({"@value" "1.0"}),
            "http://raml.org/vocabularies/http#termsOfService" '({"@value" "terms"}),
            "http://raml.org/vocabularies/http#host" '({"@value" "test.com"}),
            "http://raml.org/vocabularies/http#basePath" '({"@value" "/path"}),
            "http://raml.org/vocabularies/http#contentType" '({"@value" "appliaton/json"}),
            "http://raml.org/vocabularies/http#accepts" '({"@value" "application/json"}),
            "@type" '("http://raml.org/vocabularies/http#APIDocumentation"
                      "http://raml.org/vocabularies/document#DomainElement"),
            "http://schema.org/description" '({"@value" "description"}),
            "http://schema.org/name" '({"@value" "name"})})
        (generator/to-jsonld api-documentation false))))
