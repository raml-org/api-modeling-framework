(ns api-modelling-framework.generators.domain.common
  (:require [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]
            [api-modelling-framework.utils :as utils]
            [clojure.string :as string]))

(defn find-traits [model context syntax]
  (let [extends (document/extends model)]
    (condp = syntax
      ;; @todo do I need both checks, label and is-trait-tag ??
      :openapi (->> extends
                    (filter (fn [extend]
                              (= "trait" (name (document/label extend)))))
                    (map (fn [trait]
                           (let [trait-id (document/target trait)
                                 trait-ref (if (utils/same-doc? (document/id model) trait-id)
                                             (utils/hash-path trait-id)
                                             trait-id)]
                             {:$ref trait-ref}))))
      :raml    (->> extends
                    (filter (fn [extend]
                              (= "trait" (name (document/label extend)))))
                    (map (fn [trait]
                           (let [trait-tag (first (document/find-tag trait document/is-trait-tag))]
                             (if (some? trait-tag)
                               (document/value trait-tag)
                               (-> (document/target trait) (string/split #"/") last)))))))))

(defn annotation-reference? [model]
  (-> model
      (document/find-tag document/is-annotation-tag)
      first
      some?))

(defn trait-reference? [model]
  (satisfies? domain/Operation model))

(defn model->annotationTypes [declares context domain-generator]
  (->> declares
       (filter (fn [declare] (some? (-> declare (document/find-tag document/is-annotation-tag) first))))
       (mapv (fn [annotation]
               [(-> annotation (document/find-tag document/is-annotation-tag) first document/value) (domain-generator annotation context)]))
       (into {})))

(defn model->traits [{:keys [references] :as ctx} domain-generator]
  (->> references
       (filter (fn [ref] (nil? (:from-library ref))))
       (filter trait-reference?)
       (filter some?)
       (map (fn [reference]
              (let [generated (domain-generator reference (assoc ctx :from-library (:from-library reference)))]
                [(keyword (document/name reference)) generated])))
       (into {})))

(defn type-reference-name [reference]
  (let [is-type-tag (-> reference
                        (document/find-tag document/is-type-tag)
                        first)
        type-name (-> is-type-tag
                      (document/value))]
    type-name))

(defn model->types [{:keys [references] :as ctx} domain-generator]
  (->> references
       (filter (fn [ref] (nil? (:from-library ref))))
       (filter (fn [reference]
                 (let [is-type-tag (or (first (document/find-tag reference document/is-type-tag))
                                       (if (satisfies? domain/Type reference) reference nil))]
                   (some? is-type-tag))))
       (map (fn [reference]
              (let [generated (domain-generator reference (assoc ctx :from-library (:from-library reference)))]
                (if (satisfies? domain/Type reference)
                  [(keyword (document/name reference)) generated]
                  (let [type-name (type-reference-name reference)]
                    [(keyword type-name) generated])))))
       (into {})
       (utils/clean-nils)))

(defn remove-extension [lib]
  (if (string/index-of lib ".")
    (-> lib (string/split #"\.") first)
    lib))

(defn anon-lib-alias [lib counter]
  (try
    (let [alias (-> lib
                    document/id
                    (string/split #"/")
                    last
                    remove-extension)]
      (if (nil? alias)
        (str "lib" (swap! counter inc))
        alias))
    (catch #?(:cljs js/Error :clj Exception) ex
      (str "lib" (swap! counter inc)))))

(defn model->uses [node]
  (->> (document/find-tag node document/uses-library-tag)
       (map (fn [tag] [(remove-extension (document/name tag)) (document/value tag)]))
       (into {})))


(defn type-reference? [model]
  (-> model
      (document/find-tag document/is-type-tag)
      first
      some?))

(defn ref-shape? [shape-type {:keys [references]}]
  (->> references
       (filter (fn [ref]
                 (satisfies? domain/Type ref)))
       (filter (fn [type]
                 (= (get (domain/shape type) "@id") shape-type)))
       first))

(defn merge-fragment [base-element fragment {:keys [document-generator] :as ctx}]
  (let [encoded-fragment (document/encodes fragment)
        encoded-fragment (reduce (fn [acc k]
                                   (let [v (get base-element k)]
                                     (if (nil? v) acc (assoc acc k v))))
                                 encoded-fragment
                                 (keys base-element))
        encoded-fragment (assoc encoded-fragment :extends [])
        fragment (assoc fragment :encodes encoded-fragment)]
    (document-generator fragment ctx)))

(defn unique-alises [uses counter aliases-pairs]
  (let [aliases (atom (->> uses keys (reduce (fn [acc e] (assoc acc e true)) {})))]
    (->> aliases-pairs
         (map (fn [[alias lib]]
                (if (nil? (get @aliases alias))
                  (do (swap! aliases (fn [old-value] (assoc old-value alias true)))
                      [alias lib])
                  (let [alias (str alias "_" (swap! counter inc))]
                    (swap! aliases (fn [old-value] (assoc old-value alias true)))
                    [alias lib])))))))

(defn process-anonymous-libraries
  "Some libraries will not have a source map, in this case we find them in the list of references and we generated an identifier"
  [uses model]
  (let [uses-map (->> uses (map (fn [[alias lib]] [(document/id lib) true])) (into {}))
        refs (document/references model)
        all-libraries (->> refs (filter (fn [ref] (satisfies? document/Module ref))))
        anonymous-libraries (->> all-libraries (filter (fn [lib] (nil? (get uses-map (document/id lib))))))
        counter (atom 0)
        uses-anonymous-libraries (->> anonymous-libraries
                                      (map (fn [lib] [(anon-lib-alias lib counter) lib]))
                                      (unique-alises uses counter)
                                      (into {}))]
    (merge uses uses-anonymous-libraries)))

(defn process-anonymous-libraries-list
  "Some libraries will not have a source map, in this case we find them in the list of references and we generated an identifier"
  [uses model]
  (let [uses-map (->> uses (map (fn [lib] [(document/id lib) true])) (into {}))
        refs (document/references model)
        all-libraries (->> refs (filter (fn [ref] (satisfies? document/Module ref))))
        anonymous-libraries (->> all-libraries (filter (fn [lib] (nil? (get uses-map (document/id lib))))))]
    (concat uses anonymous-libraries)))

(defn default-label [declaration]
  (cond
    (or (nil? declaration)
        (not (satisfies? document/Node declaration)))  (str (gensym "label"))
    (some? (document/name declaration)) (document/name declaration)
    :else  (let [id (or (document/id declaration) (str gensym "label"))
                 path (last (string/split id #"/"))
                 label (remove-extension path)]
             label)))

(defn maybe-value [tag]
  (if (some? tag)
    (utils/safe-str (document/value tag))
    nil))

(defn update-alias [declare alias]
  (let [is-type-tag (-> declare (document/find-tag document/is-type-tag) first)
        is-trait-tag (-> declare (document/find-tag document/is-trait-tag) first)]
    (if (or is-trait-tag is-type-tag)
      (let [label (or (maybe-value is-type-tag)
                      (maybe-value is-trait-tag))
            old-tag (or is-type-tag is-trait-tag (document/map->IsTypeTag {:id (document/id declare) :value label}))
            new-tag (assoc old-tag :value (str (utils/safe-str alias) "." label))]
        (document/replace-tag declare old-tag new-tag))
      (let [label (default-label declare)
            is-type-tag (document/map->IsTypeTag {:id (document/id declare) :value (str (utils/safe-str alias) "." label)})
            sources (or (document/sources declare) [])
            source-map (document/->DocumentSourceMap (str (document/id declare) "/source-map")
                                                     (document/id declare)
                                                     [is-type-tag]
                                                     [])]
        (assoc declare :sources (concat sources [source-map]))))))
