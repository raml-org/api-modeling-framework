(ns raml-framework.parser.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.generators.document.jsonld :as generator]
            [raml-framework.parser.document.jsonld :as parser]
            [raml-framework.utils :as utils]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->Document {:location location
                                     :encodes nil
                                     :declares nil
                                     :document-type "open-api"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Document))
    (is (= doc parsed))))


(deftest fragment-test
  (let [location "http://test.com/location.json"
        doc (document/map->Fragment {:location location
                                     :encodes nil
                                     :document-type "open-api"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (utils/has-class? generated v/document:Fragment))
    (is (= doc parsed))))
