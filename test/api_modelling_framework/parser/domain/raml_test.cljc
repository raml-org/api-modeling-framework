(ns api-modelling-framework.parser.domain.raml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.parser.domain.raml :as raml-parser]
            [api-modelling-framework.generators.domain.raml :as raml-genenerator]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.model.domain :as domain]))

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


(deftest parse-ast-traits
  (let [node {:title "Github API"
              :baseUri "http://api.github.com"
              :version "v3"
              :traits {:paged
                       {:queryParameters
                        {:start {:type "number"}}}}
              (keyword "/users") {:displayName "Users"
                                  :get {:description "get description"
                                        :is ["paged"]
                                        :protocols ["http"]
                                        :responses {"200" {:description "200 response"}
                                                    "400" {:description "400 response"}}}}}
        location "file://path/to/resource.raml#"
        declarations (raml-parser/process-traits node {:location (str location "")
                                                       :parsed-location (str location "/declares")})
        parsed (raml-parser/parse-ast node {:parsed-location location
                                            :location location
                                            :path "/users"
                                            :references declarations
                                            :is-fragment false})
        extensions (-> parsed domain/endpoints first domain/supported-operations first document/extends)]
    (is (= 1 (count extensions)))
    (is (= "file://path/to/resource.raml#/declares/traits/paged"
           (document/target (first extensions))))))

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



(deftest parse-ast-methods
  (let [node {:displayName "Users"
              :get {:displayName "get method"
                    :description "get description"
                    :protocols ["http"]}
              :post {:displayName "post method"
                     :description "post description"
                     :protocols ["http"]}}
        parsed (raml-parser/parse-ast node {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                            :location "file://path/to/resource.raml#/users"
                                            :path "/users"
                                            :is-fragment false})
        operations (domain/supported-operations (first parsed))]
    (is (= 2 (count operations)))
    (is (= ["get" "post"] (->> operations (map domain/method))))
    (is (= ["get method" "post method"] (->> operations (map document/name))))
    (is (= ["get description" "post description"] (->> operations (map document/description))))))


(deftest parse-ast-responses
  (let [node {:displayName "Users"
              :get {:displayName "get method"
                    :description "get description"
                    :protocols ["http"]
                    :responses {200 {:description "200 response"}
                                400 {:description "400 response"}}}}
        parsed (first (raml-parser/parse-ast node {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                                   :location "file://path/to/resource.raml#/users"
                                                   :path "/users"
                                                   :is-fragment false}))]
    (is (= 2 (count (-> parsed
                        domain/supported-operations
                        first
                        domain/responses))))
    (is (= ["200" "400"] (->> parsed
                              domain/supported-operations
                              first
                              domain/responses
                              (map domain/status-code))))))


(deftest parse-ast-response-bodies
  (let [node {:displayName "get method"
              :description "get description"
              :protocols ["http"]
              :headers {:Zencoder-Api-Key {:type "string"}}
              :body {:type "string"}
              :queryParameters {:page {:type "integer"
                                       :required true}
                                :per_page {:type "integer"}}
              :responses {200 {:description "200 response"
                               :body {"application/json" {:type "string"}
                                      "text/plain"       {:type "string"}}}
                          400 {:description "400 response"
                               :body {:type "string"}}}}
        parsed (raml-parser/parse-ast node {:parsed-location "file://path/to/resource.raml#/api-documentation/resources/0"
                                            :location "file://path/to/resource.raml#/users"
                                            :path "/users"
                                            :method "get"
                                            :is-fragment false})
        responses (-> parsed (domain/responses))]
    (is (= 3 (count responses)))
    (is (= ["200" "200" "400"] (->> responses
                                    (map domain/status-code))))
    (is (= ["application/json" "text/plain" nil]
           (->> responses
                (map domain/content-type)
                flatten)))
    (is (= 1 (-> parsed domain/headers count)))
    (is (= "Zencoder-Api-Key" (-> parsed domain/headers first document/name)))
    (is (= (v/xsd-ns "string")
           (-> parsed domain/headers first domain/shape (utils/extract-jsonld (v/sh-ns "dataType") #(get % "@id")))))
    (is (= ["page" "per_page"] (->> parsed domain/request domain/parameters (map document/name))))
    (is (= [(v/xsd-ns "integer") (v/xsd-ns "integer")])
        (->> parsed domain/request domain/parameters (map #(-> % domain/shape
                                                               (utils/extract-jsonld (v/sh-ns "dataType") (fn [t] (get t "@id")))))))
    (is (= (v/xsd-ns "string")
           (-> parsed domain/request domain/schema (domain/shape) (utils/extract-jsonld (v/sh-ns "dataType") #(get % "@id")))))))


(deftest parser-ast-type-scalars
  (let [int-type {:type "integer"}
        string-type {:type "string"}
        time-only-type {:type "time-only"}
        date-only-type {:type "date-only"}]
    (doseq [raml-type [int-type string-type time-only-type date-only-type]]
      (let [shape (raml-parser/parse-ast raml-type {:parsed-location "/response"
                                                    :location "/response"})]
        (is (= raml-type (raml-genenerator/to-raml shape {})))))))


(deftest parser-ast-type-arrays
  (let [int-type {:type "array"
                  :items {:type "integer"}}
        string-type {:type "array"
                     :items {:type "string"}}
        time-only-type {:type "array"
                        :items {:type "time-only"}}
        date-only-type {:type "array"
                        :items {:type "date-only"}}
        tuple-type      {:type "array"
                         :items {:type "union"
                                 :of [{:type "date-only"}
                                      {:type "string"}]}
                         (keyword "(is-tuple)") true}]
    (doseq [raml-type [int-type string-type time-only-type date-only-type tuple-type]]
      (let [shape (raml-parser/parse-ast raml-type {:parsed-location "/response"
                                                    :location "/response"})]
        (is (= raml-type (raml-genenerator/to-raml shape {})))))))


(deftest parser-ast-type-objects
  (let [int-type {:type "array"
                  :items {:type "integer"}}
        string-type {:type "array"
                     :items {:type "string"}}
        object-type-1 {:type "object"
                       :properties {:a int-type
                                    :b string-type}
                       :additionalProperties false}
        object-type-2 {:type "object"
                       :properties {:a int-type
                                    :b string-type}}]
    (doseq [raml-type [object-type-1 object-type-2]]
      (let [shape (raml-parser/parse-ast raml-type {:parsed-location "/response"
                                                    :location "/response"})]
        (is (= raml-type (raml-genenerator/to-raml shape {})))))))
