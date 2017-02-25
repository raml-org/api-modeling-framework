(ns api-modelling-framework.generators.syntax.json
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))

  #?(:cljs (:require [cljs.nodejs :as nodejs]
                     [api-modelling-framework.utils :as utils]
                     [api-modelling-framework.platform :as platform]
                     [clojure.string :as string]
                     [api-modelling-framework.model.syntax :as syntax]
                     [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [cljs.core.async :refer [<! >! chan]]))

  #?(:clj (:require [api-modelling-framework.model.syntax :as syntax]
                    [api-modelling-framework.utils :as utils]
                    [api-modelling-framework.platform :as platform]
                    [clojure.core.async :refer [<! >! go]]
                    [clojure.walk :refer [keywordize-keys stringify-keys]]
                    [clojure.string :as string])))


(defn include-fragment [location fragment]
  (let [fragment-lcation (syntax/<-location fragment)]
    (if (and (string/starts-with? fragment-lcation location)
             (string/includes? fragment-lcation "#"))
      ("$ref" (last (string/split fragment-lcation #"#")))
      {"$ref" location})))

(defn generate-ast
  ([ast {:keys [location inline-fragments] :as context}]
   (cond
     (map? ast)  (->> ast
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
