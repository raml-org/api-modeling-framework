(ns api-modeling-framework.data
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [api-modeling-framework.utils :as utils]
            #?(:clj [clojure.core.async :refer [<! >! go chan] :as async]
               :cljs [cljs.core.async :refer [<! >! chan] :as async])
            [api-modeling-framework.platform :as platform]
            [api-modeling-framework.parser.syntax.json :as json]
            [api-modeling-framework.parser.syntax.yaml :as yaml]
            [api-modeling-framework.parser.domain.raml-types-shapes :as raml-types]
            [api-modeling-framework.parser.domain.json-schema-shapes :as json-schema]
            [api-modeling-framework.parser.domain.common :as common]))


(defn parse-payload
  ([x]
   (utils/annotation->jsonld (common/purge-ast (platform/decode-json x))))
  ([x s] ;; this needs to be fixes later
   (utils/annotation->jsonld x)))

(def generic-schema "http://raml.org/vocabularies/data#AnonShape")
(def generic-payload "http://raml.org/vocabularies/data#AnonInstance")

(defn parse-schema
  ([s t]
   (go (condp = t
         "json" (let [parsed-doc (<! (json/parse-string generic-schema s true))
                      parsed (get parsed-doc (keyword "@data"))
                      parsed(common/purge-ast parsed)]
                  (json-schema/parse-type parsed
                                          {:parsed-location generic-schema
                                           :location generic-schema
                                           :references {}}))
         "raml" (let [parsed-doc (<! (yaml/parse-string generic-schema s {:keywordize true}))
                      parsed (dissoc (get parsed-doc (keyword "@data")) :uses)
                      parsed (common/purge-ast parsed)]
                  (raml-types/parse-type parsed
                                         {:parsed-location generic-schema
                                          :location generic-schema
                                          :default-type "object"
                                          :references {}}))
         (throw (new #?(:clj Exception :cljs js/Error) (str "Unknown type of schema " t)))))))

(defn has-class? [m class]
  (let [types (get m "@type" [])]
    (->> types
         (filter #(= % class))
         first
         some?)))

(def report-class "http://www.w3.org/ns/shacl#ValidationReport")
(def conforms "http://www.w3.org/ns/shacl#conforms")
(def to-validate "http://raml.org/vocabularies/shapes#toValidate")
(def targetObejctsOf "http://www.w3.org/ns/shacl#targetObjectsOf")
(def result "http://www.w3.org/ns/shacl#result")

(defn report [results]
  (->> results
       (filter #(has-class? % report-class))
       first))

(defn conforms? [results]
  (= "true"
     (-> (or (report results) {})
         (utils/find-value conforms)
         str)))

(defn build-validation-result [m]
  {:result-path (-> m (get "http://www.w3.org/ns/shacl#resultPath" {}) first (get "@id"))
   :message (-> m (get "http://www.w3.org/ns/shacl#resultMessage" {}) first (get "@value"))
   :focus (-> m (get "http://www.w3.org/ns/shacl#focusNode" {}) first (get "@id"))
   :shape (-> m (get "http://www.w3.org/ns/shacl#sourceShape" {}) first (get "@id"))
   :severity (-> m (get "http://www.w3.org/ns/shacl#resultSeverity" {}) first (get "@id"))
   :constraint (-> m (get "http://www.w3.org/ns/shacl#sourceConstraintComponent" {}) first (get "@id"))})

(defn validation-results [results]
  (let [report (or (report results) {})
        result-ids (utils/find-links report result)
        results-map (reduce (fn [acc e] (assoc acc e true)) {} result-ids)]
    (->> results
         (filter #(some? (get results-map (get % "@id"))))
         (map build-validation-result))))

(defn validate
  "Validates a schema agains a JSON payload"
  [schema schema-type payload]
  (go (let [schema (<! (parse-schema schema schema-type))
            schema (assoc schema targetObejctsOf {"@id" to-validate})
            ;;_ (println "SCHEMA")
            ;;_ (prn schema)
            payload (parse-payload payload)
            payload {to-validate payload}
            ;;_ (println "DATA")
            ;;_ (prn payload)
            report (<! (platform/validate schema payload))]
        {:conforms (conforms? report)
         :validation-results (validation-results report)})))
