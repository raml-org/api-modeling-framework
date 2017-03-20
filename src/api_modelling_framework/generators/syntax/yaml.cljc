(ns api-modelling-framework.generators.syntax.yaml
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [cljs.nodejs :as nodejs]
                     [api-modelling-framework.utils :as utils]
                     [api-modelling-framework.platform :as platform]
                     [clojure.string :as string]
                     [api-modelling-framework.model.syntax :as syntax]
                     [clojure.walk :refer [keywordize-keys]]
                     [cljs.core.async :refer [<! >! chan]]))

  #?(:clj (:require [api-modelling-framework.model.syntax :as syntax]
                    [api-modelling-framework.utils :as utils]
                    [api-modelling-framework.platform :as platform]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [keywordize-keys]]
                    [clojure.string :as string])))

#?(:cljs (def __dirname (js* "__dirname")))
#?(:cljs (def js-yaml (nodejs/require (str __dirname "/../../../../js/js-yaml/index"))))

(def key-orders {"title" 0
                 "description" 1
                 "version" 2
                 "baseUri" 3
                 "types" 4
                 "traits" 5})

#?(:clj (defn generate-yaml-string [ast]
          (let [yaml (org.yaml.snakeyaml.Yaml.)]
            (.dump yaml (utils/ramlify ast))))
   :cljs (defn generate-yaml-string [ast]
           (.dump js-yaml (clj->js (utils/ramlify ast)) (clj->js {"sortKeys" (fn [ka kb]
                                                                               (cond
                                                                                 (and (string/starts-with? ka "/")
                                                                                      (string/starts-with? kb "/"))  0
                                                                                 (string/starts-with? ka "/")        1
                                                                                 (string/starts-with? kb "/")        -1
                                                                                 :else  (let [pos-a (get key-orders ka 100)
                                                                                              pos-b (get key-orders kb 100)]
                                                                                          (compare pos-a pos-b))))}))))

(defn include-fragment [fragment]
  (let [location (syntax/<-location fragment)]
    (str "!include " location)))


(defn generate-ast
  ([ast {:keys [location inline-fragments] :as context}]
   (cond
     (map? ast)  (->> ast
                      (mapv (fn [[k v]]
                              (cond
                                ;; libraries are not regular includes, we need to treat them in a different way
                                (= :uses k)          [k (->> v
                                                             (mapv (fn [[k l]] [k (syntax/<-location l)]))
                                                             (into {}))]
                                (syntax/fragment? v) [k (if inline-fragments
                                                          (generate-ast v)
                                                          (include-fragment v))]
                                :else                [k (generate-ast v context)])))
                      (into {}))
     (coll? ast) (mapv #(generate-ast % context) ast)

     :else       ast))

  ([ast]
   (let [data (syntax/<-data ast)
         location (syntax/<-location ast)]
     (generate-ast data {:location location}))))

(defn generate-string
  ([ast context] (generate-yaml-string (generate-ast ast context)))
  ([ast] (generate-yaml-string (generate-ast ast))))

(defn generate-file
  ([location ast context]
   (platform/write-location location
    (generate-yaml-string (generate-ast ast context))))
  ([location ast]
   (platform/write-location location (generate-yaml-string (generate-ast ast)))))
