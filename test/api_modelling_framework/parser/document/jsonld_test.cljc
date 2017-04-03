(ns api-modelling-framework.parser.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.generators.document.jsonld :as generator]
            [api-modelling-framework.generators.document.raml :as raml-generator]
            [api-modelling-framework.parser.document.raml :as raml-parser]
            [api-modelling-framework.parser.document.jsonld :as parser]
            [api-modelling-framework.utils :as utils]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedDocument {:location location
                                           :encodes nil
                                           :declares []
                                           :references []
                                           :document-type "open-api"
                                           :id "http://test.com/location.json"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Document))
    (is (= doc parsed))))


(deftest fragment-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedFragment {:location location
                                           :encodes nil
                                           :document-type "open-api"
                                           :references []
                                           :id "http://test.com/location.json"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Fragment))
    (is (= doc parsed))))


(deftest fragments-test-2
  (let [fragment {(keyword "@location") "file://path/to/get_method.raml"
                  (keyword "@data") {:displayName "get method"
                                     :description "get description"
                                     :protocols ["http"]}}
        parsed (raml-parser/parse-ast fragment {})
        json-generated (generator/to-jsonld parsed true)
        model-parsed (parser/from-jsonld json-generated)
        generated-fragment (raml-generator/to-raml model-parsed {})]
    (is (= ((keyword "@fragment") generated-fragment) "#%RAML 1.0 Fragment"))
    (is (= fragment (dissoc generated-fragment (keyword "@fragment"))))))
