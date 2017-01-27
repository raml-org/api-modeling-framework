(ns raml-framework.parser.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.generators.document.jsonld :as generator]
            [raml-framework.parser.document.jsonld :as parser]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->Document {:location location
                                     :encodes nil
                                     :declares nil
                                     :document-type "open-api"})
        generated (generator/to-jsonld doc true)
        parsed (parser/from-jsonld generated)]
    (is (parser/has-class? generated v/model:Document))
    (is (= doc parsed))))
