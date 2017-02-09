(ns api-modelling-framework.parser.document.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.domain.openapi :as domain-parser]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn parse-ast-dispatch-function [node context]
  (cond
    (and (some? (get node (keyword "@location")))
         (some? (get node (keyword "@fragment")))) :root

    (some? (get node (keyword "@location")))       :fragment

    (and (nil? (get node (keyword "@location")))
         (nil? (get node (keyword "@fragment"))))  (throw
                                                    (new #?(:clj Exception :cljs js/Error)
                                                         (str "Unsupported parsing unit, missing @location or @fragment information")))

    :else                                          nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defmethod parse-ast :root [node context]
  (let [location (get node (keyword "@location"))
        _ (debug "Parsing OpenAPI Document at " location)
        fragments (or (:fragments context) (atom {}))
        encoded (domain-parser/parse-ast (get node (keyword "@data")) {:location (str location "#")
                                                                       :fragments fragments
                                                                       :parsed-location (str location "#")
                                                                       :document-parser parse-ast
                                                                       :is-fragment false})]
    (document/map->ParsedDocument (merge context
                                         {:id location
                                          :location location
                                          :encodes encoded
                                          :references (vals @fragments)
                                          :document-type "OpenAPI"}))))

(defmethod parse-ast :fragment [node context]
  (let [context (or context {})
        location (get node (keyword "@location"))
        _ (debug "Parsing RAML Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        ;; @todo is this illegal?
        references (or (:references context) {})
        encoded (domain-parser/parse-ast (get node (keyword "@data")) (merge context
                                                                             {:location (str location "#")
                                                                              :fragments fragments
                                                                              :references references
                                                                              :parsed-location (str location "#")
                                                                              :document-parser parse-ast
                                                                              :is-fragment true}))]
    (document/map->ParsedFragment {:id location
                                   :location location
                                   :encodes encoded
                                   :references (vals @fragments)
                                   :document-type "OpenApi"})))
