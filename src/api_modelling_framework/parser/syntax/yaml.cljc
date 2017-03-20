(ns api-modelling-framework.parser.syntax.yaml
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [cljs.nodejs :as nodejs]
                     [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [cljs.core.async :refer [<! >! chan]]
                     [clojure.string :as string]))

  #?(:clj (:require [clj-yaml.core :as yaml]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [stringify-keys]]
                    [clojure.string :as string])))


#?(:cljs (enable-console-print!))
#?(:cljs (def __dirname (js* "__dirname")))
#?(:cljs (def yaml (nodejs/require (str __dirname "/../../../../js/yaml"))))



(declare parse-file)

(defn resolve-path [location path]
  (let [last-component (-> location (string/split #"/") last)
        base (string/replace location last-component "")]
    (if (or (= (string/index-of path "/") 0)
            (some? (string/index-of path "://")))
      path
      (str base path))))

(defn resolve-libraries [location parsed]
  (go (let [uses (:uses parsed {})
            uses (loop [acc []
                        libraries uses]
                   (if (empty? libraries)
                     acc
                     (let [[alias path] (first libraries)
                           library-content (<! (parse-file (resolve-path location path)))]
                       (recur (concat acc [[alias library-content]])
                              (rest libraries)))))
            uses (into {} uses)]
        (assoc parsed :uses uses))))

;; This function is only used by the JS parser.
;; We transform the additional property into
;; lexical meta-data for the parsed node.
;; The Java version in clj-yaml already generates the
;; lexical meta-data from the AST information
(defn add-location-meta [node]
  (cond
    (map? node)  (let [location (get node (keyword "__location__"))]
                   (if (some? location)
                     (with-meta
                       (->> (dissoc node (keyword "__location__"))
                            (mapv (fn [[k v]] [k (add-location-meta v)]))
                            (into {}))
                       location)
                     (->> (dissoc node (keyword "__location__"))
                          (mapv (fn [[k v]] [k (add-location-meta v)]))
                          (into {}))))
    (coll? node) (mapv add-location-meta node)
    :else        node))

#?(:cljs (defn parse-file [uri]
           (let [ch (chan)]
             (.parseYamlFile yaml uri (fn [e result]
                                        (go (try (if e
                                                   (>! ch (ex-info (str e) e))
                                                   (>! ch (->> result js->clj keywordize-keys add-location-meta)))
                                                 (catch #?(:cljs js/Error :clj Exception) ex ex)))))
             ch))
   :clj (defn parse-file [uri]
          (go (try (let [header (with-open [rdr (clojure.java.io/reader uri)] (.readLine rdr))
                         header (if (string/starts-with? (or header "") "#%RAML")
                                  header
                                  nil)
                         file (java.io.File. uri)
                         parsed (yaml/parse-file uri true)]
                     (-> {}
                         (assoc (keyword "@data") (<! (resolve-libraries (.getAbsolutePath file) parsed)))
                         (assoc (keyword "@location") (.getAbsolutePath file))
                         (assoc (keyword "@fragment") header)))
                   (catch #?(:cljs js/Error :clj Exception) ex ex)))))

#?(:cljs (defn parse-string [uri string]
           (let [ch (chan)]
             (.parseYamlString yaml uri string (fn [e result]
                                                 (go (try (if e
                                                            (>! ch (ex-info (str e) e))
                                                            (>! ch (->> result js->clj keywordize-keys add-location-meta)))
                                                          (catch #?(:cljs js/Error :clj Exception) ex ex)))))
             ch))
   :clj (defn parse-string [uri string]
          (go (try (let [header (first (string/split-lines string))
                         file (java.io.File. uri)
                         header (if (string/starts-with? (or header "") "#%RAML")
                                  header
                                  nil)
                         parsed (yaml/parse-string string uri true)]
                     (-> {}
                         (assoc (keyword "@data") (<! (resolve-libraries (.getAbsolutePath file) parsed)))
                         (assoc (keyword "@location") (.getAbsolutePath file))
                         (assoc (keyword "@fragment") header)))
                   (catch #?(:cljs js/Error :clj Exception) ex ex)))))
