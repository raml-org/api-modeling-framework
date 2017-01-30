(ns raml-framework.parser.domain.raml-test
  (:require [clojure.test :refer :all]
            [raml-framework.parser.domain.raml :as raml-parser]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]))

(deftest guess-type-test
  (is (= :root (raml-parser/guess-type {:title "a"
                                   :description "hey"})))
  (is (= :root (raml-parser/guess-type {"/path/to/resource" nil})))
  (is (= :undefined (raml-parser/guess-type {})))
  (is (= :undefined (raml-parser/guess-type {:foo :bar}))))


(deftest parse-ast-root
  (let [node {:title "GithHub API"
              :baseUri "https://api.github.com"
              :version "v3"
              :mediaType ["application/json" "application/xml"]
              :protocols ["http" "https"]
              (keyword "/users") {:displayName "Users"
                                  :get {}}}
        parsed (raml-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                            :parsed-location "file://path/to/resource.raml#"
                                            :is-fragment false})]
    (is (satisfies? domain/APIDocumentation parsed))
    (is (= "api.github.com" (domain/host parsed)))
    (is (= ["http" "https"] (domain/scheme parsed)))
    (is (= "" (domain/base-path parsed)))
    (is (= ["application/json" "application/xml"] (domain/content-type parsed)))
    (is (= ["application/json" "application/xml"] (domain/accepts parsed)))
    (is (= 1 (count (domain/nested-endpoints parsed))))))


(deftest parse-ast-resources
  (let [node {:displayName "Users"
              (keyword "/items") {:displayName "items"
                                  (keyword "/prices") {:displayName "prices"
                                                       :get {}}
                                  :get {}}
              :get {}}
        parsed (raml-parser/parse-ast node {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                            :location "file://path/to/resource.raml#/users"
                                            :path "/users"
                                            :is-fragment false})]
    (is (= "file://path/to/resource.raml#/users"
           (-> parsed (document/sources) first (document/source))))
    (is (= 1
           (-> parsed (domain/nested-endpoints) count)))
    (is (= 1
           (-> parsed (domain/nested-endpoints) first (domain/nested-endpoints) count)))
    (is (= 0
           (-> parsed
               (domain/nested-endpoints)
               first
               (domain/nested-endpoints)
               first
               (domain/nested-endpoints)
               count)))))
