(ns api-modelling-framework.model.document-test
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            [api-modelling-framework.model.document :as document]))



(deftest document-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedDocument {:location location
                                           :encodes nil
                                           :declares nil
                                           :document-type "open-api"})
        source-map (first (document/sources doc))]
    (is (= (document/id doc) location))
    (is (= location (document/source source-map)))
    (is (= 2 (count (document/tags source-map))))
    (is (= [document/file-parsed-tag document/document-type-tag] (map document/tag-id (document/tags source-map))))
    (is (= ["http://test.com/location.json" "open-api"] (map document/value (document/tags source-map))))))


(deftest fragment-test
  (let [location "http://test.com/location.json"
        doc (document/map->ParsedFragment {:location location
                                           :encodes nil
                                           :document-type "open-api"})
        source-map (first (document/sources doc))]
    (is (= (document/id doc) location))
    (is (= location (document/source source-map)))
    (is (= 2 (count (document/tags source-map))))
    (is (= [document/file-parsed-tag document/document-type-tag] (map document/tag-id (document/tags source-map))))
    (is (= ["http://test.com/location.json" "open-api"] (map document/value (document/tags source-map))))))
