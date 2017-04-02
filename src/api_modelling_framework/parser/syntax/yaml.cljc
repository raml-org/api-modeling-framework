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
            (some? (string/index-of path "://"))
            (string/index-of path base 0))
      path
      (str base path))))

(defn resolve-libraries [location parsed options]
  (go (let [uses (:uses parsed {})
            uses (loop [acc []
                        libraries uses]
                   (if (empty? libraries)
                     acc
                     (let [[alias path] (first libraries)
                           library-content (<! (parse-file (resolve-path location path) options))]
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

#?(:cljs (defn parse-file
           ([uri options]
            (let [ch (chan)]
              (.parseYamlFile yaml uri (clj->js options) (fn [e result]
                                                           (go (try (if e
                                                                      (>! ch (ex-info (str e) e))
                                                                      (>! ch (->> result js->clj keywordize-keys add-location-meta)))
                                                                    (catch #?(:cljs js/Error :clj Exception) ex ex)))))
              ch))
           ([uri] (parse-file uri {})))
   :clj (defn parse-file
          ([uri options]
           (go (try (let [parsed (yaml/parse-file uri (assoc options :keywordize true))
                          data (get parsed (keyword "@data"))
                          data (<! (resolve-libraries uri data options))]
                      (assoc parsed (keyword "@data") data))
                    (catch #?(:cljs js/Error :clj Exception) ex ex))))
          ([uri] (parse-file uri {}))))

#?(:cljs (defn parse-string
           ([uri string options]
            (let [ch (chan)]
              (.parseYamlString yaml uri string (clj->js options) (fn [e result]
                                                                    (go (try (if e
                                                                               (>! ch (ex-info (str e) e))
                                                                               (>! ch (->> result js->clj keywordize-keys add-location-meta)))
                                                                             (catch #?(:cljs js/Error :clj Exception) ex ex)))))
              ch))
           ([uri string] (parse-string uri string {})))
   :clj (defn parse-string
          ([uri string options]
           (go (try (let [parsed (yaml/parse-string string uri (assoc options :keywordize true))
                          data (get parsed (keyword "@data"))
                          data (<! (resolve-libraries uri data options))]
                      (assoc parsed (keyword "@data") data))
                    (catch #?(:cljs js/Error :clj Exception) ex ex))))
          ([uri string] (parse-string uri string {}))))
