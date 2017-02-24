(ns api-modelling-framework.generators.domain.common
  (:require [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [clojure.string :as string]))

(defn find-traits [model context]
  (let [extends (document/extends model)]
    ;; @todo do I need both checks, label and is-trait-tag ??
    (->> extends
         (filter (fn [extend] (= "trait" (name (document/label extend)))))
         (map (fn [trait]
                (let [trait-tag (first (document/find-tag trait document/is-trait-tag))]
                  (if (some? trait-tag)
                    (document/value trait-tag)
                    (-> (document/target trait) (string/split #"/") last))))))))

(defn model->traits [model {:keys [references] :as ctx} domain-generator]
  (->> (document/find-tag model document/inline-fragment-parsed-tag)
       (map (fn [tag]
              (let [trait-id (document/value tag)
                    reference (->> references
                                   (filter (fn [reference] (= (document/id reference) trait-id)))
                                   first)
                    is-trait-tag (-> reference
                                     (document/find-tag document/is-trait-tag)
                                     first)
                    trait-name (if is-trait-tag
                                 (-> is-trait-tag
                                     (document/value)
                                     keyword)
                                 (-> trait-id
                                     name
                                     (string/split #"/")
                                     last))
                    method (if (some? reference)
                             (domain/to-domain-node reference)
                             (throw (new #?(:cljs js/Error :clj Exception) (str "Cannot find extended trait " trait-name))))
                    generated (domain-generator method ctx)]
                [trait-name generated])))
       (into {})))
