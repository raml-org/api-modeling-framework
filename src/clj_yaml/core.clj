(ns clj-yaml.core
  (:require [clojure.string :as string])
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.reader StreamReader)
           [java.io File]))

(def ^{:dynamic true} *keywordize* true)

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml-dumper
  [& {:keys [dumper-options]}]
  (if dumper-options
    (Yaml. (apply make-dumper-options
                  (mapcat (juxt key val)
                          dumper-options)))
    (Yaml.)))

(defn make-yaml-loader [] (Yaml.))

(defn tag-location [tag]
  (let [line (.getLine tag)
        column (.getColumn tag)
        index (.getIndex tag)]
    [line column index]))

(defn node-location
  "Builds the lexical location for the parsed AST token"
  [node]
  (let [[start-line start-column start-index] (tag-location (.getStartMark node))
        [end-line end-column end-index] (tag-location (.getEndMark node))]
    {:start-line start-line
     :start-column start-column
     :start-index start-index
     :end-line end-line
     :end-column end-column
     :end-index end-index}))

;; Multi methods for processing recursively the AST adding the
;; lexical meta-data when required

(defn node->ast-dispatch-fn
  "We dispatch based on the type of node with the exception of the !include tag"
  [n]
  (let [tag-name (.getValue (.getTag n))]
    (if (= tag-name "!include")
      tag-name
      (str (.getNodeId n)))))

(defmulti node->ast (fn [node file] (node->ast-dispatch-fn node)))

(defmethod node->ast "mapping" [node file]
  (let [location (node-location node)
        values (.getValue node)
        parsed (->> values
                    (mapv (fn [tuple-value]
                            (let [key (node->ast (.getKeyNode tuple-value) file)
                                  value (node->ast (.getValueNode tuple-value) file)]
                              [(if *keywordize* (keyword key) key) value])))
                    (into {}))]
    (with-meta parsed location)))

(defmethod node->ast "scalar" [node _]
  (.getValue node))


(defmethod node->ast "sequence" [node file]
  (mapv node->ast (.getValue node) file))


(declare parse-file)

(defn fragment-info [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (let [first-line (first (line-seq rdr))]
      (if (string/starts-with? first-line "#%RAML")
        first-line
        nil))))

(defmethod node->ast "!include" [node file]
  (let [next-file (.getValue node)]
    (if (string/starts-with? next-file File/separator)
      (parse-file next-file)
      (let [current-file (java.io.File. file)
            parent (.getAbsolutePath (.getParentFile current-file))
            location (str parent File/separator next-file)
            fragment (fragment-info location)]
        {(keyword "@fragment") fragment
         (keyword "@location") location
         (keyword "@data") (parse-file location)
         (keyword "@raw") (slurp current-file)}))))

(defn parse-string-ast [s base-file]
  (let [node (.compose (make-yaml-loader) (java.io.StringReader. s))]
    (node->ast node base-file)))

;; top level parsing/generation functions

(defn generate-string [data & opts]
  (.dump (apply make-yaml-dumper opts) data))

(defn parse-string
  ([string base-file keywordize]
   (binding [*keywordize* keywordize]
     (parse-string-ast string base-file)))
  ([string base-file]
   (parse-string string base-file true)))

(defn parse-file-ast [uri]
  (let [file (java.io.File. uri)
        node (.compose (make-yaml-loader) (java.io.FileReader. file))]
    (node->ast node uri)))


(defn parse-file
  ([uri keywordize]
   (binding [*keywordize* keywordize]
     (parse-file-ast uri)))
  ([uri]
   (parse-file uri true)))
