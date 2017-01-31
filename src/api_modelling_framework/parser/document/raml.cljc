(ns api-modelling-framework.parser.model.raml
  (:require [clojure.string :as string]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.domain.raml :as domain-parser]))

(defn parse-ast-dispatch-function [type node]
  (cond
    (and (nil? type)
         (some? (get node "@location"))
         (some? (get node "@fragment")))      (string/trim (get node "@fragment"))
    (and (nil? type)
         (some? (get node "@location")))      :fragment
    (and (nil? type)
         (nil? (get node "@location"))
         (nil? (get node "@fragment")))       (throw (new #?(:clj Exception :cljs js/Error) (str "Unsupported parsing unit, missing @location or @fragment information")))
    :else                                     nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defmethod parse-ast "#%RAML 1.0" [_ node]
  (let [location (get node "@location")
        encoded (domain-parser/parse-ast node {:location (str location "#")
                                               :parsed-location (str location "#")
                                               :is-fragment true})]
    (document/->Document location encoded nil "RAML")))

(defmethod parse-ast :fragment [_ node]
  (let [location (get node "@location")
        encoded (domain-parser/parse-ast node {:location (str location "#")
                                               :parsed-location (str location "#")
                                               :is-fragment true})]
    (document/->Fragment location encoded "RAML")))
