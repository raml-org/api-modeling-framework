(ns api-modeling-framework.run
  (:gen-class)
  (:require [api-modeling-framework.core :as core]
            [api-modeling-framework.utils :as utils]
            [clojure.core.async :refer [<!!]])
  (:import [org.topbraid.spin.util JenaUtil]
           [java.io StringWriter]
           [org.apache.commons.io IOUtils]
           [java.nio.charset Charset]
           [org.apache.jena.riot RDFDataMgr RDFFormat]
           [org.apache.jena.util FileUtils]))


(defn to-n3 [data]
  (let [m (JenaUtil/createMemoryModel)
        writer (StringWriter.)]
    (.read m (IOUtils/toInputStream data (Charset/defaultCharset)) "urn:dummy" "JSON-LD")
    (RDFDataMgr/write writer m RDFFormat/NTRIPLES)
    (.toString writer)))

(defn to-jsonld [data]
  (let [m (JenaUtil/createMemoryModel)
        writer (StringWriter.)]
    (.read m (IOUtils/toInputStream data (Charset/defaultCharset)) "urn:dummy" "JSON-LD")
    (RDFDataMgr/write writer m RDFFormat/JSONLD_EXPAND_PRETTY)
    (.toString writer)))

(defn check-error [x]
  (when-let [e (or (some? (:err x)) (some? (:error x)))]
    (if (instance? Throwable x)
      (throw x)
      (throw (Exception. (str x))))))

(defn -main [format path & args]
  (println "Parsing vocabulary at " path " generating " format)
  (let [parser (core/->RAMLParser)
        model (<!! (utils/cb->chan (partial core/parse-file parser path)))
        _ (check-error model)
        output-model (core/document-model model)
        jsonld-generator (core/->APIModelGenerator)
        output-jsonld (<!! (utils/cb->chan (partial core/generate-string jsonld-generator path
                                                    output-model
                                                    {:source-maps? false})))]
    (check-error output-jsonld)
    (println (condp = format
               "n3"     (to-n3 output-jsonld)
               "jsonld" (to-jsonld output-jsonld)))))
