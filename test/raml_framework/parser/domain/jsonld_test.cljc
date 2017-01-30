(ns raml-framework.parser.domain.jsonld-test
  (:require [clojure.test :refer :all]
            [raml-framework.model.vocabulary :as v]
            [raml-framework.parser.domain.jsonld :as jsonld-parser]
            [raml-framework.model.domain :as domain]
            [raml-framework.model.document :as document]))

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
