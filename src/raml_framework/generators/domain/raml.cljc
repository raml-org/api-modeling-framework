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
    (and (satisfies? domain/EndPoint model)
         (satisfies? document/Node model))       domain/EndPoint
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

(defn find-children-resources
  "Find the children for a particular model using the information stored in the tags"
  [id children]
  (->> children
       (filter (fn [child] (= (-> child
                                 (document/find-tag document/nested-resource-parent-id-tag)
                                 first
                                 document/value)
                             id)))))

(defn merge-children-resources
  "We merge the children in the current node using the paths RAML style"
  [node children-resources ctx]
  (merge node
         (->> children-resources
              (map (fn [child]
                     (let [child-path (-> child
                                          (document/find-tag document/nested-resource-path-parsed-tag)
                                          first)
                           ;; the node might come from a OpenAPI model, it will not have path tag
                           child-path (if (some? child-path)
                                        (document/value child-path)
                                        (domain/path child))]
                       [(keyword child-path) (to-raml child ctx)])))
              (into {}))))

(defmethod to-raml domain/APIDocumentation [model ctx]
  (debug "Generating RAML root node")
  (let [all-resources (domain/endpoints model)
        ctx (assoc ctx :all-resources all-resources)
        children-resources (find-children-resources (document/id model) all-resources)
        ;; this can happen if we are generating from a model that does not generate the tags,
        ;; e.g. OpenAPI, in this case all the children resources are children of the APIDocumentation node
        children-resources (if (empty? children-resources) all-resources children-resources)]
    (-> {:title (document/name model)
         :description (document/description model)
         :version (domain/version model)
         :baseUri (model->base-uri model)
         :protocols (model->protocols model)
         :mediaType (model->media-type model)}
        (merge-children-resources children-resources ctx)
        utils/clean-nils)))


(defmethod to-raml domain/EndPoint [model {:keys [all-resources] :as ctx}]
  (debug "Generating resource " (document/id model))
  (let [children-resources (find-children-resources (document/id model) all-resources)]
    (-> {:displayName (document/name model)
         :description (document/description model)}
        (merge-children-resources children-resources ctx)
        utils/clean-nils)))

(defmethod to-raml nil [_ _]
  (debug "Generating nil")
  nil)
