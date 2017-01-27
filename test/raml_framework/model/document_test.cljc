(ns raml-framework.model.document-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [raml-framework.model.document :as document]))



(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->Document {:location location
                                     :encodes nil
                                     :declares nil
                                     :document-type "open-api"})
        source-map (first (document/sources doc))]
    (prn source-map)
    (is (= (document/id doc) location))
    (is (= location (document/location source-map)))
    (is (= 2 (count (document/tags source-map))))
    (is (= [document/file-parsed-tag document/document-type-tag] (map document/tag-id (document/tags source-map))))
    (is (= ["http://test.com/location.json" "open-api"] (map document/value (document/tags source-map))))))
