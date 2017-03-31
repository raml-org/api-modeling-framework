(ns api-modelling-framework.tck
  #?(:cljs (:require-macros [cljs.test :refer [deftest is async]]
                            [cljs.core.async.macros :refer [go]]))
  (:require [api-modelling-framework.core :as core]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.platform :as platform]
            [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.utils-test :refer [cb->chan error?]]
            [clojure.string :as string]
            #?(:cljs [cljs.nodejs :as nodejs])
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [api-modelling-framework.platform :refer [async]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            #?(:clj [clojure.test :refer [deftest is]])
            #?(:clj [clojure.data :as data])))

#?(:cljs (def fs (nodejs/require "fs")))

(defn file-exists? [f]
  #?(:clj (let [f (java.io.File. f)]
            (.exists f))
     :cljs (.existsSync fs f)))

(def raml-10-tests "resources/tck/raml-1.0")
(def tck-test-cases {:raml-01 {:api {:test001 {:raml (str raml-10-tests "/Api/test001/api.raml")
                                               :openapi  (str raml-10-tests "/Api/test001/api.openapi")
                                               :jsonld (str raml-10-tests "/Api/test001/api.jsonld")},

                                     :test003 {:raml (str raml-10-tests "/Api/test003/api.raml")
                                               :openapi  (str raml-10-tests "/Api/test003/api.openapi")
                                               :jsonld (str raml-10-tests "/Api/test003/api.jsonld")}

                                     :test004 {:raml (str raml-10-tests "/Api/test004/api.raml")
                                               :openapi  (str raml-10-tests "/Api/test004/api.openapi")
                                               :jsonld (str raml-10-tests "/Api/test004/api.jsonld")}
                                     },
                               :resources {:test001 {:raml (str raml-10-tests "/Resources/test001/api.raml")
                                                     :openapi (str raml-10-tests "/Resources/test001/api.openapi")
                                                     :jsonld (str raml-10-tests "/Resources/test001/api.jsonld")}
                                           :test002 {:raml (str raml-10-tests "/Resources/test002/api.raml")
                                                     :openapi (str raml-10-tests "/Resources/test002/api.openapi")
                                                     :jsonld (str raml-10-tests "/Resources/test002/api.jsonld")}
                                           }
                               :responses {:test002 {:raml (str raml-10-tests "/Responses/test002/api.raml")
                                                     :openapi (str raml-10-tests "/Responses/test002/api.openapi")
                                                     :jsonld (str raml-10-tests "/Responses/test002/api.jsonld")}
                                           :test003 {:raml (str raml-10-tests "/Responses/test003/api.raml")
                                                     :openapi (str raml-10-tests "/Responses/test003/api.openapi")
                                                     :jsonld (str raml-10-tests "/Responses/test003/api.jsonld")}}}})
(def tools {:raml {:parser (core/->RAMLParser)
                   :generator (core/->RAMLGenerator)}
            :openapi {:parser (core/->OpenAPIParser)
                      :generator (core/->OpenAPIGenerator)}
            :jsonld {:parser (core/->APIModelParser)
                     :generator (core/->APIModelGenerator)}})
(def conversions (->> (for [a  [:raml :openapi :jsonld] b [:raml :openapi :jsonld]] [a b])
                      (filter (fn [[a b]] (not= a b)))))

(defn enumerate-tests []
  (->> (:raml-01 tck-test-cases)
       (map (fn [[type tests]]
              (->> tests
                   (map (fn [[test-name files]]
                          [[type test-name] files])))))
       (apply concat)
       (into {})))

(defn -success-> [x]
  (is (not (error? x)))
  x)

;; only for data diffs on error
(defn clean-noise [x]
  (cond
    (map? x)  (->> (dissoc x "@type")
                   (mapv (fn [[k v]] [(if (string/index-of k "#") (last (string/split k #"#"))  k)
                                     (clean-noise v)]))
                   (into {}))
    ;; order is not important for comparisons
    (coll? x) (into #{} (mapv clean-noise x))
    :else     x))

(defn same-structure? [a b]
  #?(:clj (when (not= a b)
            (println "ERROR IN STRUCTURAL COMPARISON")
            (println "\nGENERATED:")
            (clojure.pprint/pprint (clean-noise a))
            (println "\nTARGET:")
            (clojure.pprint/pprint (clean-noise b))
            (println "\nDIFF:\n")
            (clojure.pprint/pprint (data/diff (clean-noise a) (clean-noise b)))))
  (= a b))

(defn clean-ids [x]
  (cond
    (map? x)  (->> (dissoc x "@id")
                   (mapv (fn [[k v]] [k (clean-ids v)]))
                   (into {}))
    (coll? x) (into #{} (mapv clean-ids x))
    :else     x))


(defn fragment? [x]
  (and (map? x)
       (some? (first (filter #(= % (keyword "@data")) (keys x))))))

(defn clean-fragments [x]
  (cond
    (and
     (fragment? x)
     (map? x))     (->> (syntax/<-data x)
                        (mapv (fn [[k v]] [k (clean-fragments v)]))
                        (into {}))
    (map? x)       (->> x
                        (mapv (fn [[k v]] [k (clean-fragments v)]))
                        (into {}))

    (coll? x)      (mapv clean-fragments x)
    :else          x))

(defn ensure-not-nil [x]
  (is (not (nil? x)))
  x)

(defn to-data-structure [type s]
  (go (condp = type
        :raml      (->> (yaml-parser/parse-string "data.raml" s)
                        <!
                        -success->
                        clean-fragments)
        :openapi   (platform/decode-json s)
        :jsonld    (platform/decode-json s))))

(defn target-file
  ([files base type]
   (let [file (str (get files base) "." (name type))]
     (if (file-exists? file)
       file
       (get files base))))
  ([files type] (target-file files :jsonld type)))

(defn check-syntax [type files]
  (println "CHECKING SYNTAX " type)
  (go (let [parser (-> tools type :parser)
            generator (-> tools type :generator)
            jsonld-generator (-> tools :jsonld :generator)
            target (->> (target-file files type)
                        (platform/read-location)
                        <!
                        -success->
                        (platform/decode-json))
            parsed-model (<! (cb->chan (partial core/parse-file parser (get files type) {})))
            _ (is (not (error? parsed-model)))
            generated-jsonld (<! (cb->chan (partial core/generate-string jsonld-generator
                                                    (get files :jsonld)
                                                    (core/document-model parsed-model)
                                                    {:source-maps? false})))
            _ (is (not (error? parsed-model)))
            doc-a (<! (to-data-structure type (-success-> (<! (platform/read-location (get files type))))))
            doc-b (<! (to-data-structure type (-success-> (<! (cb->chan (partial core/generate-string generator
                                                                                 (get files type)
                                                                                 (core/document-model parsed-model)
                                                                                 {}))))))]
        (is (same-structure? (ensure-not-nil (clean-ids (platform/decode-json generated-jsonld)))
                             (ensure-not-nil (clean-ids target))))
        (is (same-structure? (ensure-not-nil (clean-ids doc-a))
                             (ensure-not-nil (clean-ids doc-b)))))))

(defn check-conversions [files]
  (go (doseq [[from to] conversions]
        (println "COMPARING " from " -> " to)
        (let [source (get files from)
              target (get files to)
              target (->> (target-file files to from)
                          (platform/read-location)
                          <!
                          -success->
                          (to-data-structure to)
                          <!)
              parser (-> tools from :parser)
              generator (-> tools to :generator)
              parsed-model (<! (cb->chan (partial core/parse-file parser source {:source-maps? true})))
              generated (<! (cb->chan (partial core/generate-string generator
                                               target
                                               (core/document-model parsed-model)
                                               {:source-maps? false})))]
          (is (same-structure? (ensure-not-nil (clean-ids (<! (to-data-structure to generated))))
                               (ensure-not-nil (clean-ids target))))))))

(defn focus [test tests]
  (if (= test :all)
    tests
    (->> tests
         (filter (fn [[name files]] (= test name ))))))

(deftest tck-tests
  (async done
         (go
           (doseq [[test-name files] (focus :all (enumerate-tests))]
             (println "- Testing " test-name)
             (<! (check-syntax :raml files))
             (<! (check-syntax :openapi files))
             (<! (check-syntax :jsonld files))
             (<! (check-conversions files))
             )
           (done))))
