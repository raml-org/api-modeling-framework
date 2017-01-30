(ns raml-framework.parser.model.openapi
  (:require [clojure.string :as string]
            [raml-framework.model.document :as document]
            [raml-framework.parser.domain.openapi :as domain-parser]))

(defn parse-ast-dispatch-function [type node]
  (cond
    (and (nil? type)
         (some? (get node "@location"))
         (some? (get node "@fragment")))      :root
    (and (nil? type)
         (some? (get node "@location")))      :fragment
    (and (nil? type)
         (nil? (get node "@location"))
         (nil? (get node "@fragment")))       (throw (new #?(:clj Exception :cljs js/Error) (str "Unsupported parsing unit, missing @location or @fragment information")))
    :else                                     nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defmethod parse-ast :root [_ node]
  (let [location (get node "@location")
        encoded (domain-parser/parse-ast (get node "@data") {:location (str location "#")
                                                             :parsed-location (str location "#")
                                                             :is-fragment false})]
    (document/->Document location encoded  nil "OpenApi")))

(defmethod parse-ast :fragment [_ node]
  (let [location (get node "@location")
        encoded (domain-parser/parse-ast :root (get node "@data") {:location (str location "#")
                                                                   :parsed-location (str location "#")
                                                                   :is-fragment true})]
    (document/->Fragment location encoded "OpenApi")))
