(ns raml-framework.parser.syntax.yaml
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [cljs.nodejs :as nodejs]
                     [clojure.walk :refer [keywordize-keys]]
                     [cljs.core.async :refer [<! >! chan]]))

  #?(:clj (:require [clj-yaml.core :refer [decode]]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.string :as string]))

  #?(:clj (:import [raml_framework.java IncludeConstructor])))

#?(:cljs (enable-console-print!))
#?(:cljs (def __dirname (js* "__dirname")))
#?(:cljs (def yaml (nodejs/require (str __dirname "/../../../../js/yaml"))))



#?(:cljs (defn parse-yaml [location]
           (let [ch (chan)]
             (.parseYaml yaml location (fn [e result]
                                         (go (try (if e
                                                    (>! ch (ex-info (str e) e))
                                                    (>! ch (->> result js->clj keywordize-keys)))
                                                  (catch #?(:cljs js/Error :clj Exception) ex ex)))))
             ch))
   :clj (defn parse-yaml [location]
          (go (try (let [header (with-open [rdr (clojure.java.io/reader location)] (.readLine rdr))
                         header (if (string/starts-with? (or header "") "#%RAML")
                                  header
                                  nil)
                         file (java.io.File. location)
                         include-constructor (IncludeConstructor. file)
                         yaml (org.yaml.snakeyaml.Yaml. include-constructor)
                         raw (.load yaml (java.io.FileInputStream. file))
                         parsed (decode raw)]
                     (-> parsed
                         (assoc (keyword "@location") (.getAbsolutePath file))
                         (assoc (keyword "@fragment") header)))
                   (catch #?(:cljs js/Error :clj Exception) ex ex)))))
