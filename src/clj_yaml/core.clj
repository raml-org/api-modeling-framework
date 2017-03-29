(ns clj-yaml.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (org.yaml.snakeyaml.reader StreamReader)
           [java.io File]))

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

(defmulti node->ast (fn [node file options] (node->ast-dispatch-fn node)))

(defmethod node->ast "mapping" [node file options]
  (let [location (node-location node)
        values (.getValue node)
        parsed (->> values
                    (mapv (fn [tuple-value]
                            (let [key (node->ast (.getKeyNode tuple-value) file options)
                                  value (node->ast (.getValueNode tuple-value) file options)]
                              [(if (:keywordize options) (keyword key) key) value])))
                    (into {}))]
    (with-meta parsed location)))

(defmethod node->ast "scalar" [node _ _]
  (.getValue node))


(defmethod node->ast "sequence" [node file options]
  (mapv #(node->ast % file options) (.getValue node)))


(declare parse-file)

(defn cache-resolved-uri [uri options]
  (let [cache (get options "cacheDirs" {})
        found-cache (->> cache
                         (filter (fn [[cached-uri cachedir]]
                                   (string/starts-with? uri cached-uri)))
                         first)]
    (if (some? found-cache)
      (let [[cached-uri cache-dir] found-cache]
        (string/replace uri cached-uri cache-dir))
      uri)))

(defn local-uri? [uri]
  (or (string/starts-with? uri "file://")
      (nil? (string/index-of uri "://"))))

(defn external-uri? [uri]
  (or (string/starts-with? uri "http://")
      (string/starts-with? uri "https://")))

(defn uri->reader [uri options]
  (let [cache-resolved (cache-resolved-uri uri options)]
    (if (local-uri? cache-resolved)
      (java.io.FileReader. (io/as-file cache-resolved))
      (java.io.StringReader. (slurp (io/as-url cache-resolved))))))

(defn fragment-info [file options]
  (with-open [rdr (java.io.BufferedReader. (uri->reader file options))]
    (let [first-line (first (line-seq rdr))]
      (if (string/starts-with? first-line "#%RAML")
        first-line
        nil))))

(defn fragment-info-string [s]
  (let [first-line (first (line-seq (java.io.BufferedReader. (java.io.StringReader. s))))]
    (if (string/starts-with? first-line "#%RAML")
      first-line
      nil)))

(defn parent-path [path]
  (let [current-file (java.io.File. path)]
    (.getAbsolutePath (.getParentFile current-file))))

(defn parent-url [uri]
  (let [uri (java.net.URI. uri)]
    (-> (if (string/ends-with? (.getPath uri) "/")
          (.resolve uri "..")
          (.resolve uri "."))
        str
        (string/replace #"/$" ""))))

(defn resolve-path [file next-file]
  (str (if (local-uri? file)
         (parent-path file)
         (parent-url file))
       File/separator
       next-file))

(defmethod node->ast "!include" [node file options]
  (let [next-file (.getValue node)]
    (if (or (external-uri? next-file)
            (string/starts-with? next-file File/separator))
      (parse-file next-file options)
      (let [location (resolve-path file next-file)
            fragment (fragment-info location options)]
        {(keyword "@fragment") fragment
         (keyword "@location") location
         (keyword "@data") (parse-file location options)
         (keyword "@raw") (slurp (uri->reader location options))}))))

(defn parse-string-ast
  ([s base-file options]
   (let [node (.compose (make-yaml-loader) (java.io.StringReader. s))]
     (node->ast node base-file options))))

;; top level parsing/generation functions

(defn generate-string [data & opts]
  (.dump (apply make-yaml-dumper opts) data))

(defn wrap-parsing-result
  ([location parsed options raw]
   (let [fragment (fragment-info-string raw)]
     {(keyword "@fragment") fragment
      (keyword "@location") location
      (keyword "@data") parsed
      (keyword "@raw") raw}))
  ([location parsed options]
   (wrap-parsing-result location parsed options (slurp (uri->reader location options)))))

(defn parse-string
  ([string base-file options]
   (wrap-parsing-result base-file
                        (parse-string-ast string base-file options)
                        options
                        string))
  ([string base-file]
   (parse-string string base-file {:keywordize true})))

(defn parse-file-ast
  ([uri options]
   (let [node (.compose (make-yaml-loader) (uri->reader uri options))]
     (node->ast node uri options))))

(defn parse-file
  ([uri options]
   (wrap-parsing-result uri
                        (parse-file-ast uri options)
                        options))
  ([uri]
   (parse-file uri {:keywordize true})))
