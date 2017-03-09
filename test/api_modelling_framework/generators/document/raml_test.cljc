(ns api-modelling-framework.generators.document.raml-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.document.raml :as parser]
            [api-modelling-framework.generators.document.raml :as generator]))
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
                                            {:start {:type "number"}}}}
                                  (keyword "/users") {:displayName "Users"
                                                      :post {:description "post description"
                                                             :is ["paged"]
                                                             :responses {"201" {:description "201 response"}
                                                                         "400" {:description "400 response"}}}
                                                      :get {(keyword "@location") "file://path/to/get_method.raml"
                                                            (keyword "@data") {:description "get description"
                                                                               :protocols ["http"]
                                                                               :responses {"200" {:description "200 response"}
                                                                                           "400" {:description "400 response"}}}
                                                            (keyword "@fragment") "#%RAML 1.0 Fragment"}}}}
        parsed (parser/parse-ast input {})
        generated (generator/to-raml parsed {})]
    (is (= generated input))))


(deftest generate-libraries-test
  (let [location "file://path/to/library.raml"
        input {(keyword "@location") location
               (keyword "@fragment") "#%RAML 1.0 Library"
               (keyword "@data") {:usage "Use to define some basic file-related constructs."
                                  :types {:File {:type "object"
                                                 :properties {:name {:type "string"}
                                                              :length {:type "integer"}}}}
                                  :traits {:drm {:headers {:drm-key {:type "string"}}}}}}
        parsed (parser/parse-ast input {})
        generated (generator/to-raml parsed {})]
    (is (= generated input))))

(deftest generate-libraries-2-test
  (let [location "file://path/to/library.raml"
        input-library {(keyword "@location") location
                       (keyword "@fragment") "#%RAML 1.0 Library"
                       (keyword "@data") {:usage "Use to define some basic file-related constructs."
                                          :types {:File {:type "object"
                                                         :properties {:name {:type "string"}
                                                                      :length {:type "integer"}}}}
                                          :traits {:drm {:headers {:drm-key {:type "string"}}}}}}
        input {(keyword "@location") "file://path/to/api.raml"
               (keyword "@fragment") "#%RAML 1.0"
               (keyword "@data") {:uses {:lib input-library}
                                  (keyword "/test") {:get {:responses {"200" {:body "lib.File"}}}}}}
        parsed (parser/parse-ast input {})
        generated (generator/to-raml parsed {})]
    (is (= generated input))))
