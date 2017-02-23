(ns api-modelling-framework.generators.domain.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.parser.domain.raml :as raml-parser]
            [api-modelling-framework.parser.document.raml :as raml-document-parser]
            [api-modelling-framework.generators.domain.jsonld :as generator]
            [api-modelling-framework.parser.domain.jsonld :as parser]
            [api-modelling-framework.generators.domain.raml :as raml-generator]
            [api-modelling-framework.generators.document.raml :as document-raml-generator]))

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

(deftest to-raml-endpoints-test
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
        generated (generator/to-jsonld api-documentation true)]
    (is (= 3 (-> generated (get v/http:endpoint) count)))
    (let [endpoints (-> generated (get v/http:endpoint))]
      (doseq [endpoint endpoints]
        (let [sources-count (-> endpoint (get v/document:source) count)]
          (is (or (= 3 sources-count)
                  (= 4 sources-count)))))
      (is (= ["/users" "/users/items" "/users/items/prices"]
             (->> endpoints
                  (map #(first (get % v/http:path)))
                  (map #(get % "@value"))))))))


(deftest from-raml-traits-test
  (let [input {:baseUri "http://test.com"
               :protocols "http"
               :version "1.0"
               :traits {:secure {:queryParameters {:access_token {:type "string"}}}}
               (keyword "/users") {:displayName "Users"
                                   (keyword "/items") {:displayName "items"
                                                       :is ["secure"]}}}
        traits (raml-parser/process-traits input {:location "file://location/#"
                                                  :fragments []
                                                  :document-parser raml-document-parser/parse-ast
                                                  :parsed-location (str "file://location/#/declares")})
        api-documentation (raml-parser/parse-ast input
                                                 {:location "file://path/to/resource.raml#"
                                                  :parsed-location "file://path/to/resource.raml#"
                                                  :references traits
                                                  :is-fragment false})
        generated (generator/to-jsonld api-documentation true)
        parsed    (parser/from-jsonld generated)
        output (raml-generator/to-raml parsed {:references (vals traits)
                                               :fragments {}
                                               :document-generator document-raml-generator/to-raml})]
    (is (= input output))))
