(ns raml-framework.parser.domain.raml-test
  (:require [clojure.test :refer :all]
            [raml-framework.parser.domain.raml :as raml-parser]
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
              :protocols ["http" "https"]}
        parsed (raml-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                            :parsed-location "file://path/to/resource.raml#"
                                            :is-fragment false})]
    (is (satisfies? domain/APIDocumentation parsed))
    (is (= "api.github.com" (domain/host parsed)))
    (is (= ["http" "https"] (domain/scheme parsed)))
    (is (= "" (domain/base-path parsed)))
    (is (= ["application/json" "application/xml"] (domain/content-type parsed)))
    (is (= ["application/json" "application/xml"] (domain/accepts parsed)))))
