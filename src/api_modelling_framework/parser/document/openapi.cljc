(ns api-modelling-framework.parser.model.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.domain.openapi :as domain-parser]))

(defn parse-ast-dispatch-function [type node]
  (cond
    (and (nil? type)
         (some? (get node (keyword "@location")))
         (some? (get node (keyword "@fragment"))))      :root

    (and (nil? type)
         (some? (get node (keyword "@location"))))      :fragment

    (and (nil? type)
         (nil? (get node (keyword "@location")))
         (nil? (get node (keyword "@fragment"))))       (throw
                                                         (new #?(:clj Exception :cljs js/Error)
                                                              (str "Unsupported parsing unit, missing @location or @fragment information")))

    :else                                               nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defmethod parse-ast :root [_ node]
  (let [location (get node (keyword "@location"))
        fragments (atom {})
        encoded (domain-parser/parse-ast (get node (keyword "@data")) {:location (str location "#")
                                                                       :fragments fragments
                                                                       :parsed-location (str location "#")
                                                                       :is-fragment false})]
    (document/->ParsedDocument location encoded  nil "OpenApi")))

(defmethod parse-ast :fragment [_ node]
  (let [location (get node (keyword "@location"))
        fragments (atom {})
        encoded (domain-parser/parse-ast :root (get node (keyword "@data")) {:location (str location "#")
                                                                             :fragments fragments
                                                                             :parsed-location (str location "#")
                                                                             :is-fragment true})]
    (document/->ParsedFragment location encoded "OpenApi")))
