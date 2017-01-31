(ns raml-framework.parser.domain.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.parser.domain.openapi :as openapi-parser]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]))


(deftest parse-ast-root
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
              :paths {(keyword "/users") {:get {}}}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                               :parsed-location "file://path/to/resource.raml#"
                                               :is-fragment false})]
    (is (satisfies? domain/APIDocumentation parsed))
    (is (= "api.test.com" (domain/host parsed)))
    (is (= ["http" "https"] (domain/scheme parsed)))
    (is (= "/test/endpoint" (domain/base-path parsed)))
    (is (= ["application/ld+json"] (domain/content-type parsed)))
    (is (= ["application/json" "application/xml"] (domain/accepts parsed)))
    (is (= 1 (count (domain/endpoints parsed))))
    (is (= 2 (count (-> parsed
                        (domain/endpoints)
                        first
                        (document/sources)))))
    (is (= "/users" (domain/path (first (domain/endpoints parsed)))))))



(deftest parse-ast-methods
  (let [node {:get {:operationId "get"
                    :description "get description"
                    :schemes ["https"]
                    :tags ["experimantl" "foo" "bar"]
                    :produces ["application/ld+json"]
                    :consumes ["application/json"]}
              :post {:operationId "post"
                     :description "post description"
                     :schemes ["https"]
                     :tags ["experimantl" "foo" "bar"]
                     :produces ["application/ld+json"]
                     :consumes ["application/json"]}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                               :parsed-location "file://path/to/resource.raml#"
                                               :is-fragment false
                                               :path "/Users"})
        operations (domain/supported-operations parsed)]
    (is (= 2 (count operations)))
    (is (= ["experimantl" "foo" "bar" "experimantl" "foo" "bar"]
           (->> operations
                (map (fn [op] (document/find-tag op document/api-tag-tag)))
                flatten
                (map (fn [tag] (document/value tag))))))
    (is (= ["get" "post"] (->> operations
                               (map domain/method))))))
