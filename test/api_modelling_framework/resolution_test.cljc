(ns api-modelling-framework.resolution-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.resolution :as resolution]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.document.raml :as raml-parser]
            [api-modelling-framework.parser.document.openapi :as openapi-parser]
            [api-modelling-framework.parser.document.jsonld :as jsonld-parser]
            [api-modelling-framework.generators.domain.jsonld :as jsonld-generator]
            [api-modelling-framework.generators.domain.raml :as raml-generator]
            [api-modelling-framework.generators.domain.openapi :as openapi-genenerator]
            ))


(deftest generate-fragments
  (let [location "file://path/to/resource.raml"
        input {(keyword "@location") location
               (keyword "@fragment") "#%RAML 1.0"
               (keyword "@data") {:title "Github API"
                                  :baseUri "http://api.github.com"
                                  :protocols "http"
                                  :version "v3"
                                  :traits {:paged
                                           {:queryParameters
                                            {:start {:type "float"}}}}
                                  (keyword "/users") {:displayName "Users"
                                                      :post {:description "post description"
                                                             :is ["paged"]
                                                             :responses {"201" {:description "201 response"}
                                                                         "400" {:description "400 response"}}}
                                                      :get {(keyword "@location") "file://path/to/get_method.raml"
                                                            (keyword "@data") {:description "get description"
                                                                               :protocols ["http"]
                                                                               :responses {"200" {:description "200 response"}
                                                                                           "400" {:description "400 response"}}}}}}}
        parsed (raml-parser/parse-ast input {})
        document-resolved (resolution/resolve parsed {})
        resolved (document/encodes document-resolved)
        generated-jsonld (jsonld-generator/to-jsonld resolved {})
        generated-raml (raml-generator/to-raml resolved {})
        generated-openapi (openapi-genenerator/to-openapi resolved {})]

    (is (document/resolved document-resolved))
    ;; testing generted raml
    (is (= (sort [:displayName :get :post]) (sort (-> generated-raml (get (keyword "/users")) keys))))
    (is (= ["http"] (-> generated-raml (get (keyword "/users")) :get :protocols)))
    (is (= ["200" "400"] (-> generated-raml (get (keyword "/users")) :get :responses keys)))
    (is (= ["http"] (-> generated-raml (get (keyword "/users")) :post :protocols)))
    (is (= ["201" "400"] (-> generated-raml (get (keyword "/users")) :post :responses keys)))
    (is (= "number" (-> generated-raml (get (keyword "/users")) :post :queryParameters :start)))

    ;; testing generted jsonld
    (is (= (sort [:get :post]) (sort (-> generated-openapi :paths (get (keyword "/users")) keys))))
    (is (= ["http"] (-> generated-openapi :paths (get (keyword "/users")) :get :schemes)))
    (is (= ["200" "400"] (-> generated-openapi :paths (get (keyword "/users")) :get :responses keys)))
    (is (= ["http"] (-> generated-openapi :paths (get (keyword "/users")) :post :schemes)))
    (is (= ["201" "400"] (-> generated-openapi :paths (get (keyword "/users")) :post :responses keys)))
    (is (= {:name "start", :required true, :in "query", :type "number"} (-> generated-openapi :paths (get (keyword "/users")) :post :parameters first)))

    ;; testing generted JSON-LED
    (is (= "/users" (-> generated-jsonld (get v/http:endpoint) first (get v/http:path) first (get "@value"))))
    (is (= 2 (-> generated-jsonld (get v/http:endpoint) first (get v/hydra:supportedOperation) count)))
    (is (= (sort ["get" "post"]) (sort (-> generated-jsonld (get v/http:endpoint) first (get v/hydra:supportedOperation)
                                           (->> (map #(-> % (get v/hydra:method) first (get "@value"))))))))
    (is (= ["200" "400" "201" "400"])
        (-> generated-jsonld (get v/http:endpoint) first (get v/hydra:supportedOperation)
            (->> (map #(-> % (get v/hydra:returns))))
            flatten
            (->> (map #(-> % (get v/hydra:statusCode) first (get "@value"))))))
    (is (= ["start"] (-> generated-jsonld (get v/http:endpoint) first (get v/hydra:supportedOperation)
                         (->> (map #(-> % (get v/hydra:expects))))
                         flatten
                         (->> (map #(-> % (get v/http:parameter))))
                         flatten
                         (->> (filter some?) (map #(-> (get % v/sorg:name) first (get "@value")))))))
    ))
