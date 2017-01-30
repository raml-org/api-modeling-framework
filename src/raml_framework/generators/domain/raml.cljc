(ns raml-framework.generators.domain.raml
  (:require [raml-framework.model.vocabulary :as v]
            [raml-framework.model.document :as document]
            [raml-framework.model.domain :as domain]
            [raml-framework.utils :as utils]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs refer-macros)
             [debug]]))

(defn to-raml-dispatch-fn [model ctx]
  (cond
    (nil? model)                                 model
    (and (satisfies? domain/APIDocumentation model)
         (satisfies? document/Node model))       domain/APIDocumentation
    :else                                        (type model)))

(defmulti to-raml (fn [model ctx] (to-raml-dispatch-fn model ctx)))

(defn model->base-uri [model]
  (let [scheme (domain/scheme model)
        host (domain/host model)
        base-path (domain/base-path model)]
    (if (and (some? scheme) (not (empty? scheme)) (some? host))
      (str (first scheme) "://" host base-path)
      nil)))

(defn model->protocols [model]
  (let [schemes (domain/scheme model)]
    (cond
      (and (some? schemes)
           (= 1 (count schemes))) (first schemes)
      (some? schemes)             schemes
      :else                       nil)))

(defn model->media-type [model]
  (let [contents (or (domain/content-type model) [])
        accepts (or (domain/accepts model) [])
        media-types (concat contents accepts)]
    (cond
      (empty? media-types)      nil
      (nil? media-types)        nil
      (= 1 (count media-types)) (first media-types)
      :else                     media-types)))

(defmethod to-raml domain/APIDocumentation [model ctx]
  (debug "Generating RAML root node")
  (-> {:title (document/name model)
       :description (document/description model)
       :version (domain/version model)
       :baseUri (model->base-uri model)
       :protocols (model->protocols model)
       :mediaType (model->media-type model)}
      utils/clean-nils))

(defmethod to-raml nil [_ _]
  (debug "Generating nil")
  nil)
