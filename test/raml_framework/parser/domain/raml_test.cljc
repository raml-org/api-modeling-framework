(ns raml-framework.parser.domain.raml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.parser.domain.raml :as raml-parser]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]))

(deftest guess-type-test
  (is (= :root (raml-parser/guess-type {:title "a"
                                   :description "hey"})))
  (is (= :root (raml-parser/guess-type {(keyword "/path/to/resource") nil})))
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
    (is (= 1 (count (domain/endpoints parsed))))
    (is (= ["file://path/to/resource.raml#/%2Fusers" "/users" "file://path/to/resource.raml#/api-documentation"]
           (->> parsed
                (domain/endpoints)
                first
                (document/sources)
                (map document/tags)
                flatten
                (map document/value))))))


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
    (is (= 3 (count parsed)))
    (is (= "file://path/to/resource.raml#/users"
           (-> parsed (nth 0) (document/sources) first (document/source))))
    (is (= "file://path/to/resource.raml#/users/%2Fitems"
           (-> parsed (nth 1) (document/sources) first (document/source))))
    (is (= "file://path/to/resource.raml#/users/%2Fitems/%2Fprices"
           (-> parsed (nth 2) (document/sources) first (document/source))))
    (is (= 1 (count (-> parsed (nth 0) (document/find-tag document/nested-resource-path-parsed-tag)))))
    (is (= 1 (count (-> parsed (nth 1) (document/find-tag document/nested-resource-path-parsed-tag)))))
    (is (= 1 (count (-> parsed (nth 2) (document/find-tag document/nested-resource-path-parsed-tag)))))
    (is (= nil (-> parsed (nth 0) (document/find-tag document/nested-resource-path-parsed-tag) first document/value)))
    (is (= "/items" (-> parsed (nth 1) (document/find-tag document/nested-resource-path-parsed-tag) first document/value)))
    (is (= "/prices" (-> parsed (nth 2) (document/find-tag document/nested-resource-path-parsed-tag) first document/value)))
    (is (= (-> parsed (nth 0) (document/find-tag document/nested-resource-children-tag) first document/value)
           (-> parsed (nth 1) document/id)))
    (is (= (-> parsed (nth 1) (document/find-tag document/nested-resource-children-tag) first document/value)
           (-> parsed (nth 2) document/id)))
    (is (= (-> parsed (nth 1) document/id)
           (-> parsed (nth 2) (document/find-tag document/nested-resource-parent-id-tag) first document/value)))
    (is (= (-> parsed (nth 0) document/id)
           (-> parsed (nth 1) (document/find-tag document/nested-resource-parent-id-tag) first document/value)))
    (is (= (nil? (-> parsed (nth 0) (document/find-tag document/nested-resource-parent-id-tag) first document/value))))))
