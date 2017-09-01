(ns api-modeling-framework.data-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]
                            [cljs.test :refer [deftest is async]]))
  (:require #?(:clj [clojure.test :refer :all])
            #?(:clj [api-modeling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [<! >! go chan]]
               :cljs [cljs.core.async :refer [<! >! chan]])
            [api-modeling-framework.data :as data]
            [api-modeling-framework.platform :refer [encode-json]]))

;; (deftest validate-test
;;   (async done
;;          (go (let [schema (encode-json {:type "object"
;;                                         :properties {:name "string"
;;                                                      :surname "string"}})
;;                    payload (encode-json {:name "John" })
;;                    payload-correct (encode-json {:name "John" :surname "Doe"})
;;                    res (<! (data/validate schema "raml" payload))
;;                    res-correct (<! (data/validate schema "raml" payload-correct))]
;;                (is (not (:conforms res)))
;;                (is (= 1 (count (:validation-results res))))
;;                (is (= "http://raml.org/vocabularies/shapes/anon#surname"
;;                       (-> res :validation-results first :result-path)))
;;                (is (= 0 (count (:validation-results res-correct))))
;;                (done)))))
;;
;;
;; (deftest validate-test-2
;;   (async done
;;          (go (let [schema (encode-json {:type "object"
;;                                         :properties {:name "string[]"}})
;;                    payload (encode-json {:name 2 })
;;                    payload-correct (encode-json {:name ["A" "B" "C"]})
;;                    res (<! (data/validate schema "raml" payload))
;;                    res-correct (<! (data/validate schema "raml" payload-correct))]
;;                ;;(println "INCORRECT")
;;                ;;(clojure.pprint/pprint res)
;;                ;;(println "CORRECT")
;;                ;;(clojure.pprint/pprint res-correct)
;;                (is (not (:conforms res)))
;;                (is (= 1 (count (:validation-results res))))
;;                (is (= "http://raml.org/vocabularies/shapes/anon#name"
;;                       (-> res :validation-results first :result-path)))
;;                (is (= "http://www.w3.org/ns/shacl#DatatypeConstraintComponent"
;;                       (-> res :validation-results first :constraint)))
;;                (is (:conforms res-correct))
;;                (is (= 0 (count (:validation-results res-correct))))
;;                (done)))))
