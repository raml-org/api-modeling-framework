(ns raml-framework.generators.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.generators.document.jsonld :as generator]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->Document {:location location
                                     :encodes nil
                                     :declares nil
                                     :document-type "open-api"})
        generated (generator/to-jsonld doc true)]
    (is (= {"@id" "http://test.com/location.json",
            "@type" ["http://raml.org/vocabularies/model#Document"
                     "http://raml.org/vocabularies/model#Unit"],
            "http://raml.org/vocabularies/model#source" [{"@id" "http://test.com/location.json#/source-map/0",
                                                          "@type" ["http://raml.org/vocabularies/model#SourceMap"],
                                                          "http://raml.org/vocabularies/model#location"[{"@id" "http://test.com/location.json"}]
                                                          "http://raml.org/vocabularies/model#tag" [{"@id" "http://test.com/location.json#/source-map/0/tag/file-parsed",
                                                                                                     "@type" ["http://raml.org/vocabularies/model#Tag"],
                                                                                                     "http://raml.org/vocabularies/model#tagId"
                                                                                                     [{"@value" "file-parsed"}],
                                                                                                     "http://raml.org/vocabularies/model#tagValue"
                                                                                                     [{"@value" "http://test.com/location.json"}]}
                                                                                                    {"@id" "http://test.com/location.json#/source-map/0/tag/document-type",
                                                                                                     "@type" ["http://raml.org/vocabularies/model#Tag"],
                                                                                                     "http://raml.org/vocabularies/model#tagId"
                                                                                                     [{"@value" "document-type"}],
                                                                                                     "http://raml.org/vocabularies/model#tagValue"
                                                                                                     [{"@value" "open-api"}]}]}]}
           generated))
    (is (= ["@id" "@type" "http://raml.org/vocabularies/model#source"] (keys generated)))
    (is (= 1 (-> generated (get v/model:source) count)))
    (is (= "http://test.com/location.json"
           (-> generated (get v/model:source) first (get v/model:location) first (get "@id"))))
    (is (= 2 (-> generated (get v/model:source) first (get v/model:tag) count)))))
