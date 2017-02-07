(ns api-modelling-framework.parser.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.generators.document.jsonld :as generator]
            [api-modelling-framework.parser.document.jsonld :as parser]
            [api-modelling-framework.utils :as utils]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedDocument {:location location
                                           :encodes nil
                                           :declares nil
                                           :document-type "open-api"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Document))
    (is (= doc parsed))))


(deftest fragment-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedFragment {:location location
                                           :encodes nil
                                           :document-type "open-api"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Fragment))
    (is (= doc parsed))))
