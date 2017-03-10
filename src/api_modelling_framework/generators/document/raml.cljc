(ns api-modelling-framework.generators.document.raml
  (:require [api-modelling-framework.model.vocabulary :as v]
            [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.generators.domain.raml :as domain-generator]
            [api-modelling-framework.utils :as utils]
            [api-modelling-framework.generators.domain.common :as common]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros)
             [debug]]))

(defn to-raml-dispatch-fn [model ctx]
  (cond
    (nil? model)                                  model

    (and (satisfies? document/Fragment model)
         (satisfies? document/Module model))      :document

    (satisfies? document/Fragment model)          :fragment

    (satisfies? document/Module model)            :library))


(defmulti to-raml (fn [model ctx] (to-raml-dispatch-fn model ctx)))

(defn update-alias [declare alias]
  (let [is-type-tag (-> declare (document/find-tag document/is-type-tag) first)
        is-trait-tag (-> declare (document/find-tag document/is-trait-tag) first)
        old-tag (or is-type-tag is-trait-tag)
        new-tag (assoc old-tag :value (str (utils/safe-str alias) "." (utils/safe-str (document/value old-tag))))]
    (document/replace-tag declare old-tag new-tag)))

(defmethod to-raml :document [model ctx]
  (debug "Generating Document at " (document/location model))
  (let [fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          [alias (get fragments location)]))
                  (into {}))
        declares (document/declares model)
        library-declares (->> uses
                              (mapv (fn [[alias fragment]]
                                      (mapv #(update-alias % alias) (document/declares fragment))))
                              flatten
                              (mapv (fn [declaration] (assoc declaration :from-library true))))
        uses (->> uses
                  (mapv (fn [[alias fragment]]
                          [(keyword alias) (to-raml fragment ctx)]))
                  (into {}))
        context (-> ctx
                    (assoc :references (concat declares library-declares))
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-raml))
        encoded (domain-generator/to-raml (document/encodes model) context)
        encoded (if (> (count uses) 0) (assoc encoded :uses uses) encoded)]
    {(keyword "@location") (document/location model)
     (keyword "@data") encoded
     (keyword "@fragment") "#%RAML 1.0"}))


(defmethod to-raml :fragment [model ctx]
  (debug "Generating Fragment at " (document/location model))
  (let [fragments (if (:fragments ctx)
                    (:fragments ctx)
                    (->> (document/references model)
                         (reduce (fn [acc fragment]
                                   (assoc acc (document/location fragment) fragment))
                                 {})))
        context (-> ctx
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (or (:expanded-fragments ctx)
                                                   (atom {})))
                    (assoc :type-hint :method)
                    (assoc :document-generator to-raml))
        fragment-type-tag (first (document/find-tag model document/document-type-tag))
        fragment-type (if (some? fragment-type-tag) (document/value fragment-type-tag) nil)
        ;; Parsing this is going to be a always a DomainElement,
        ;; there's a corner case when we invoke find-element in the core/Domain interface
        ;; In this case we wrap the found domain element into a Fragment but there's
        ;; no currently logic to transform the specific domain element (APIDocumentation, Operation, etc)
        ;; into a generic DomainElement, we just assign it as we find it, that's the reason we need to
        ;; do this check here.
        ;;
        ;; @todo
        ;; Write a to-domain-element function so we can get rid of this logic and have
        ;; a unified behaviour
        encoded (document/encodes model)
        encoded (if (satisfies? domain/DomainElement encoded)
                  (domain/to-domain-node encoded)
                  encoded)
        data (domain-generator/to-raml encoded context)]
    (utils/clean-nils {(keyword "@location") (document/location model)
                       (keyword "@data") (if (string? data)
                                           ;; this is possible because the fragment can be just
                                           ;; a XML schema string or JSON-schema or documentation string
                                           data
                                           (utils/clean-nils
                                            (merge data
                                                   {:usage (document/description model)})))
                       (keyword "@fragment") fragment-type})))

(defmethod to-raml :library [model ctx]
  (debug "Generating Library at " (document/location model))
  (let [declares (document/declares model)
        fragments (->> (document/references model)
                       (reduce (fn [acc fragment]
                                 (assoc acc (document/location fragment) fragment))
                               {}))
        context (-> ctx
                    (assoc :references declares)
                    (assoc :fragments fragments)
                    (assoc :expanded-fragments (atom {}))
                    (assoc :document-generator to-raml))
        types (common/model->types (assoc context :resolve-types true) domain-generator/to-raml!)
        traits (common/model->traits context domain-generator/to-raml!)
        uses (->> (common/model->uses model)
                  (mapv (fn [[alias location]]
                          [alias (get fragments location)]))
                  (into {}))]
    {(keyword "@location") (document/location model)
     (keyword "@data") (-> {:usage (document/description model)
                            :uses uses
                            :types types
                            :traits traits}
                           (utils/clean-nils))
     (keyword "@fragment") "#%RAML 1.0 Library"}))
