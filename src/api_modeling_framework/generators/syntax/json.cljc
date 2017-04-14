(ns api-modeling-framework.generators.syntax.json
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [api-modeling-framework.utils :as utils]
                     [api-modeling-framework.platform :as platform]
                     [clojure.string :as string]
                     [api-modeling-framework.model.syntax :as syntax]
                     [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [cljs.core.async :refer [<! >! chan]]))

  #?(:clj (:require [api-modeling-framework.model.syntax :as syntax]
                    [api-modeling-framework.utils :as utils]
                    [api-modeling-framework.platform :as platform]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [keywordize-keys stringify-keys]]
                    [clojure.string :as string])))


(defn include-fragment [location fragment]
  (let [fragment-location (syntax/<-location fragment)]
    (let [res (if (and (string/starts-with? fragment-location location)
                       (string/includes? fragment-location "#"))
                ("$ref" (last (string/split fragment-location #"#")))
                {"$ref" fragment-location})]
      res)))

(defn include-libraries [document]
  (if-let [libraries (get document :x-uses)]
    (let [locations (mapv #(syntax/<-location %) libraries)]
      (assoc document "x-uses" locations))
    document))

(defn generate-ast
  ([ast {:keys [location inline-fragments] :as context}]
   (cond
     (map? ast)  (->> ast
                      (include-libraries)
                      (mapv (fn [[k v]]
                              (if (syntax/fragment? v)
                                [k (if inline-fragments
                                     (generate-ast v)
                                     (include-fragment location v))]
                                [k (generate-ast v context)])))
                      (into {}))
     (coll? ast) (mapv #(generate-ast % context) ast)

     :else       ast))

  ([ast]
   (let [data (syntax/<-data ast)
         location (syntax/<-location ast)]
     (generate-ast data {:location location}))))

(defn generate-string
  ([ast context] (platform/encode-json (utils/swaggify (generate-ast ast context))))
  ([ast] (platform/encode-json (utils/swaggify (generate-ast ast)))))

(defn generate-file
  ([location ast context]
   (platform/write-location location
    (platform/encode-json (generate-ast ast context))))
  ([location ast]
   (platform/write-location location (platform/encode-json (generate-ast ast)))))
