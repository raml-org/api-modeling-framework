(ns api-modelling-framework.parser.syntax.json-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]
                            [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [<! >! go chan]]
               :cljs [cljs.core.async :refer [<! >! chan]])
            [api-modelling-framework.parser.syntax.json :as json]))

#?(:cljs (enable-console-print!))


(deftest json-pointer-test
  (let [obj {"foo" ["bar" "baz"]
             "" 0
             "a/b" 1
             "c%d" 2,
             "e^f" 3,
             "g|h" 4,
             "i\\j" 5,
             "k\"l" 6,
             " " 7,
             "m~n" 8
             "nested" [{"value1" "error"} {"value2" 9}]}]
    (is (= obj (json/json-pointer "" obj)))
    (is (= ["bar" "baz"] (json/json-pointer "/foo" obj)))
    (is (= "bar" (json/json-pointer "/foo/0" obj)))
    (is (= 0 (json/json-pointer "/" obj)))
    (is (= 1 (json/json-pointer "/a~1b" obj)))
    (is (= 2 (json/json-pointer "/c%d" obj)))
    (is (= 3 (json/json-pointer "/e^f" obj)))
    (is (= 4 (json/json-pointer "/g|h" obj)))
    (is (= 5 (json/json-pointer "/i\\j" obj)))
    (is (= 6 (json/json-pointer "/k\"l" obj)))
    (is (= 7 (json/json-pointer "/ " obj)))
    (is (= 8 (json/json-pointer "/m~0n" obj)))
    (is (= 9 (json/json-pointer "/nested/1/value2" obj)))))

(deftest ->id-test
  (is (= "file://./resources/something" (json/->id "resources/something")))
  (is (= "http://test.com/something" (json/->id "http://test.com/something")))
  (is (= "file:///test/something" (json/->id "file:///test/something")))
  (is (= "file://../test/something" (json/->id "../test/something")))
  (is (= "file://./test/something" (json/->id "./test/something")))
  (is (= "file:///test/something" (json/->id "/test/something"))))

(deftest join-path-test
  (is (= "file:///Users/other/test/other-location"
         (json/join-path (json/->id "/Users/test/directory")  "/Users/other/test/other-location")))
  (is (= "file:///Users/test/directory#/test/other-location"
         (json/join-path (json/->id "/Users/test/directory")  "#/test/other-location")))
  (is (= "file:///Users/test/directory#after"
         (json/join-path (json/->id "/Users/test/directory#before")  "#after")))
  (is (= "file:///Users/test/other/directory/file.json"
         (json/join-path (json/->id "/Users/test/current.json")  "other/directory/file.json")))
  (is (= "file:///Users/test/other/directory/file.json"
         (json/join-path (json/->id "/Users/test/current.json")  "./other/directory/file.json")))
  (is (= "file:///Users/other/directory/file.json"
         (json/join-path (json/->id "/Users/test/current.json")  "../other/directory/file.json")))
  (is (= "http://test.com/something/else"
         (json/join-path (json/->id "/Users/test/current.json") "http://test.com/something/else")))
  (is (= "file:///Users/test/current.json"
         (json/join-path (json/->id "http://test.com/something/else") "file:///Users/test/current.json")))
  (is (= "file://./resources/addresses/aux3.json#/lala"
       (json/join-path (json/->id "file://./resources/addresses/aux2.json#/definitions/address") "./aux3.json#/lala"))))

(deftest absolute-uri-test
  (is (json/absolute-uri? "http://test.com/hey/me"))
  (is (json/absolute-uri? "file:///this/is/a/test"))
  (is (not (json/absolute-uri? "file://./this/is/a/test"))))

(deftest json-parse-1-test
  (async done
         (go (let [result (<! (json/parse-json "resources/twitter.json"))]
               (is (= "file://./resources/twitter.json"
                      (get result (keyword "@location"))))
               (is (some? (get result (keyword "@data"))))
               (done)))))


(deftest json-parse-2-test
  (async done
         (go (let [result (<! (json/parse-json "resources/addresses/api.json"))]
               (is (= "file://./resources/addresses/aux.json#/definitions/address"
                      (-> result
                          (get (keyword "@data"))
                          (get :allOf)
                          first
                          (get (keyword "@location")))))
               (is (= [:street_address :city :state]
                      (-> result
                          (get (keyword "@data"))
                          (get :allOf)
                          first
                          (get (keyword "@data"))
                          (get :properties)
                          (keys))))
               (done)))))


(deftest json-parse-3-test
  (async done
         (go (let [result (<! (json/parse-json "resources/addresses/api2.json"))]
               (is (= "file://./resources/addresses/aux2.json#/definitions/address"
                      (-> result
                          (get (keyword "@data"))
                          (get :allOf)
                          first
                          (get (keyword "@location")))))
               (is (= [:street_address :city :state]
                      (-> result
                          (get (keyword "@data"))
                          (get :allOf)
                          first
                          (get (keyword "@data"))
                          (get :properties)
                          (keys))))
               (done)))))
