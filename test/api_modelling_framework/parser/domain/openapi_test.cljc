(ns api-modelling-framework.parser.domain.openapi-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.parser.domain.openapi :as openapi-parser]
            [api-modelling-framework.generators.domain.openapi :as openapi-generator]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]))


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


(deftest parse-ast-response
  (let [node {:get {:responses {:200 {:description "200 response"}
                                :error {:description "error response"}}}}
        parsed (openapi-parser/parse-ast node {:location "file://path/to/resource.raml#"
                                               :parsed-location "file://path/to/resource.raml#"
                                               :is-fragment false
                                               :path "/Users"})]
    (is (= 2 (count (->> parsed
                         domain/supported-operations
                         first
                         domain/responses))))
    (is (= ["200" nil] (->> parsed
                            domain/supported-operations
                            first
                            domain/responses
                            (map domain/status-code))))
    (is (= ["200" "error"] (->> parsed
                                domain/supported-operations
                                first
                                domain/responses
                                (map document/name))))))


(deftest parser-ast-type-scalars
  (let [int-type {:type "number"}
        string-type {:type "string"}
        time-only-type {:type "string"
                        :x-rdf-type "xsd:time"}
        any-type {:type "string"
                  :x-rdf-type "shapes:any"}]
    (openapi-parser/parse-ast int-type {:parsed-location "/response"
                                        :location "/response"})
    (doseq [json-schema-type [int-type string-type time-only-type any-type]]
      (let [shape (openapi-parser/parse-ast json-schema-type {:parsed-location "/response"
                                                              :location "/response"})]
        (is (= json-schema-type (openapi-generator/to-openapi shape {})))))))


(deftest parser-ast-type-arrays
  (let [int-type {:type "array"
                  :items {:type "number"}}
        string-type {:type "array"
                     :items {:type "string"}}
        time-only-type {:type "array"
                        :items {:type "string"
                                :x-rdf-type "xsd:time"}}
        tuple-type {:type "array"
                    :items [{:type "string"}
                            {:type "number"}]}]
    (doseq [openapi-type [int-type string-type time-only-type tuple-type]]
      (let [shape (openapi-parser/parse-ast openapi-type {:parsed-location "/response"
                                                          :location "/response"})]
        (is (= openapi-type (openapi-generator/to-openapi shape {})))))))

(deftest parser-ast-type-objects
  (let [int-type {:type "array"
                  :items {:type "number"}}
        string-type {:type "array"
                     :items {:type "string"}}
        object-type-1 {:type "object"
                       :properties {:a int-type
                                    :b string-type}
                       :additionalProperties false}
        object-type-2 {:type "object"
                       :properties {:a int-type
                                    :b string-type}
                       :required ["a"]}]
    (doseq [raml-type [object-type-1 object-type-2]]
      (let [shape (openapi-parser/parse-ast raml-type {:parsed-location "/response"
                                                    :location "/response"})]
        (is (= raml-type (openapi-generator/to-openapi shape {})))))))
