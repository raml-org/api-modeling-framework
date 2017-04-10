(ns api-modelling-framework.parser.syntax.yaml
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [cljs.core.async :refer [<! >! chan]]
                     [clojure.string :as string]

                     ;; this will trigger adding the js-support-bundle in compilation
                     ;; for the web version and introduce JS_YAML
                     ;; it will be a noop for the node version

                     [api_modelling_framework.js-support]
                     [api-modelling-framework.parser.syntax.common :refer [add-location-meta]]))

  #?(:clj (:require [clj-yaml.core :as yaml]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [stringify-keys]]
                    [clojure.string :as string]
                    [api-modelling-framework.parser.syntax.common :refer [add-location-meta]])))

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

#?(:cljs (defn parse-file
           ([uri options]
            (let [ch (chan)]
              (JS_YAML/parseYamlFile uri (clj->js options) (fn [e result]
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
              (JS_YAML/parseYamlString uri string (clj->js options) (fn [e result]
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
