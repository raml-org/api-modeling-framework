(ns api-modelling-framework.parser.syntax.yaml
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [cljs.nodejs :as nodejs]
                     [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [cljs.core.async :refer [<! >! chan]]))

  #?(:clj (:require [clj-yaml.core :refer [decode]]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [stringify-keys]]
                    [clojure.string :as string]))

  #?(:clj (:import [api_modelling_framework.java IncludeConstructor])))

#?(:cljs (enable-console-print!))
#?(:cljs (def __dirname (js* "__dirname")))
#?(:cljs (def yaml (nodejs/require (str __dirname "/../../../../js/yaml"))))


#?(:cljs (defn parse-file [uri]
           (let [ch (chan)]
             (.parseYamlFile yaml uri (fn [e result]
                                        (go (try (if e
                                                   (>! ch (ex-info (str e) e))
                                                   (>! ch (->> result js->clj keywordize-keys)))
                                                 (catch #?(:cljs js/Error :clj Exception) ex ex)))))
             ch))
   :clj (defn parse-file [uri]
          (go (try (let [header (with-open [rdr (clojure.java.io/reader uri)] (.readLine rdr))
                         header (if (string/starts-with? (or header "") "#%RAML")
                                  header
                                  nil)
                         file (java.io.File. uri)
                         include-constructor (IncludeConstructor. file)
                         yaml (org.yaml.snakeyaml.Yaml. include-constructor)
                         raw (.load yaml (java.io.FileInputStream. file))
                         parsed (decode raw)]
                     (-> {}
                         (assoc (keyword "@data") parsed)
                         (assoc (keyword "@location") (.getAbsolutePath file))
                         (assoc (keyword "@fragment") header)))
                   (catch #?(:cljs js/Error :clj Exception) ex ex)))))


#?(:cljs (defn parse-string [uri string]
           (let [ch (chan)]
             (.parseYamlString yaml uri string (fn [e result]
                                                 (go (try (if e
                                                            (>! ch (ex-info (str e) e))
                                                            (>! ch (->> result js->clj keywordize-keys)))
                                                          (catch #?(:cljs js/Error :clj Exception) ex ex)))))
             ch))
   :clj (defn parse-string [uri string]
          (go (try (let [header (first (string/split-lines string))
                         file (java.io.File. uri)
                         header (if (string/starts-with? (or header "") "#%RAML")
                                  header
                                  nil)
                         include-constructor (IncludeConstructor. file)
                         yaml (org.yaml.snakeyaml.Yaml. include-constructor)
                         raw (.load yaml string)
                         parsed (decode raw)]
                     (-> {}
                         (assoc (keyword "@data") parsed)
                         (assoc (keyword "@location") (.getAbsolutePath file))
                         (assoc (keyword "@fragment") header)))
                   (catch #?(:cljs js/Error :clj Exception) ex ex)))))
