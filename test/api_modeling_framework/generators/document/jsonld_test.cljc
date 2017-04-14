(ns api-modeling-framework.generators.document.jsonld-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modeling-framework.model.vocabulary :as v]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.generators.document.jsonld :as generator]))


(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedDocument {:location location
                                           :encodes nil
                                           :declares nil
                                           :document-type "open-api"})
        generated (generator/to-jsonld doc true)]
    (is (= {"@id" "http://test.com/location.json",
            "@type" ["http://raml.org/vocabularies/document#Document"
                     "http://raml.org/vocabularies/document#Fragment"
                     "http://raml.org/vocabularies/document#Module"
                     "http://raml.org/vocabularies/document#Unit"],
            "http://raml.org/vocabularies/document#source" [{"@id" "http://test.com/location.json#/source-map/0",
                                                             "@type" ["http://raml.org/vocabularies/document#SourceMap"],
                                                             "http://raml.org/vocabularies/document#location"[{"@id" "http://test.com/location.json"}]
                                                             "http://raml.org/vocabularies/document#tag" [{"@id" "http://test.com/location.json#/source-map/0/tag/file-parsed",
                                                                                                           "@type" ["http://raml.org/vocabularies/document#Tag"],
                                                                                                           "http://raml.org/vocabularies/document#tagId"
                                                                                                           [{"@value" "file-parsed"}],
                                                                                                           "http://raml.org/vocabularies/document#tagValue"
                                                                                                           [{"@value" "http://test.com/location.json"}]}
                                                                                                          {"@id" "http://test.com/location.json#/source-map/0/tag/document-type",
                                                                                                           "@type" ["http://raml.org/vocabularies/document#Tag"],
                                                                                                           "http://raml.org/vocabularies/document#tagId"
                                                                                                           [{"@value" "document-type"}],
                                                                                                           "http://raml.org/vocabularies/document#tagValue"
                                                                                                           [{"@value" "open-api"}]}]}]}
           generated))
    (is (= ["@id" "@type" "http://raml.org/vocabularies/document#source"] (keys generated)))
    (is (= 1 (-> generated (get v/document:source) count)))
    (is (= "http://test.com/location.json"
           (-> generated (get v/document:source) first (get v/document:location) first (get "@id"))))
    (is (= 2 (-> generated (get v/document:source) first (get v/document:tag) count)))))


(deftest document-test2
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedFragment {:location location
                                           :encodes nil
                                           :document-type "open-api"})
        generated (generator/to-jsonld doc true)]
    (is (= {"@id" "http://test.com/location.json",
            "@type" ["http://raml.org/vocabularies/document#Fragment"
                     "http://raml.org/vocabularies/document#Unit"],
            "http://raml.org/vocabularies/document#source" [{"@id" "http://test.com/location.json#/source-map/0",
                                                          "@type" ["http://raml.org/vocabularies/document#SourceMap"],
                                                          "http://raml.org/vocabularies/document#location"[{"@id" "http://test.com/location.json"}]
                                                          "http://raml.org/vocabularies/document#tag" [{"@id" "http://test.com/location.json#/source-map/0/tag/file-parsed",
                                                                                                     "@type" ["http://raml.org/vocabularies/document#Tag"],
                                                                                                     "http://raml.org/vocabularies/document#tagId"
                                                                                                     [{"@value" "file-parsed"}],
                                                                                                     "http://raml.org/vocabularies/document#tagValue"
                                                                                                     [{"@value" "http://test.com/location.json"}]}
                                                                                                    {"@id" "http://test.com/location.json#/source-map/0/tag/document-type",
                                                                                                     "@type" ["http://raml.org/vocabularies/document#Tag"],
                                                                                                     "http://raml.org/vocabularies/document#tagId"
                                                                                                     [{"@value" "document-type"}],
                                                                                                     "http://raml.org/vocabularies/document#tagValue"
                                                                                                     [{"@value" "open-api"}]}]}]}
           generated))
    (is (= ["@id" "@type" "http://raml.org/vocabularies/document#source"] (keys generated)))
    (is (= 1 (-> generated (get v/document:source) count)))
    (is (= "http://test.com/location.json"
           (-> generated (get v/document:source) first (get v/document:location) first (get "@id"))))
    (is (= 2 (-> generated (get v/document:source) first (get v/document:tag) count)))))
