(ns api-modelling-framework.parser.document.openapi
  (:require [clojure.string :as string]
            [api-modelling-framework.model.syntax :as syntax]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.parser.domain.openapi :as domain-parser]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn parse-ast-dispatch-function [node context]
  (cond
    (and (some? (syntax/<-location node))
         (some? (syntax/<-fragment node))) :root

    (some? (syntax/<-location node))       :fragment

    (and (nil? (syntax/<-location node))
         (nil? (syntax/<-fragment node)))  (throw
                                            (new #?(:clj Exception :cljs js/Error)
                                                 (str "Unsupported parsing unit, missing @location or @fragment information")))

    :else                                          nil))

(defmulti parse-ast (fn [type node] (parse-ast-dispatch-function type node)))

(defmethod parse-ast :root [node context]
  (let [location (syntax/<-location node)
        _ (debug "Parsing OpenAPI Document at " location)
        fragments (or (:fragments context) (atom {}))
        ;; we parse traits and types and add the information into the context
        traits(domain-parser/process-traits (syntax/<-data node) {:location (str location "#")
                                                                  :fragments fragments
                                                                  :document-parser parse-ast})
        types (domain-parser/process-types (syntax/<-data node) {:location (str location "#")
                                                                 :fragments fragments
                                                                 :document-parser parse-ast})
        declarations (merge traits types)
        encoded (domain-parser/parse-ast (syntax/<-data node) {:location (str location "#")
                                                               :fragments fragments
                                                               :references declarations
                                                               :document-parser parse-ast
                                                               :is-fragment false})]
    (document/map->ParsedDocument (merge context
                                         {:id location
                                          :location location
                                          :encodes encoded
                                          :declares (vals declarations)
                                          :references (vals @fragments)
                                          :document-type "OpenAPI"}))))

(defmethod parse-ast :fragment [node context]
  (let [context (or context {})
        location (syntax/<-location node)
        _ (debug "Parsing RAML Fragment at " location)
        fragments (or (:fragments context) (atom {}))
        ;; @todo is this illegal?
        references (or (:references context) {})
        encoded (domain-parser/parse-ast (syntax/<-data node) (merge context
                                                                     {:location (str location "#")
                                                                      :fragments fragments
                                                                      :references references
                                                                      :document-parser parse-ast
                                                                      :is-fragment true}))]
    (document/map->ParsedFragment {:id location
                                   :location location
                                   :encodes encoded
                                   :references (vals @fragments)
                                   :document-type "OpenApi"})))
