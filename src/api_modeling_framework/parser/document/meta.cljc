(ns api-modeling-framework.parser.document.meta
  (:require [clojure.string :as string]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.model.vocabulary :as vocabulary]
            [api-modeling-framework.parser.domain.meta :as domain-parser]
            [api-modeling-framework.parser.domain.common :as common]
            [api-modeling-framework.parser.document.common :refer [make-compute-fragments]]
            [api-modeling-framework.utils :as utils]
            [cemerick.url :as url]
            [taoensso.timbre :as timbre
             #?(:clj :refer :cljs :refer-macros) [debug]]))


(defn ahead-references [declarations parsed-location]
  (->> declarations
       (map (fn [{:keys [declaration-name]}]
              (let [declaration-name  (url/url-encode (utils/safe-str declaration-name))
                    declaration-id (common/type-reference parsed-location declaration-name)]
                [(keyword declaration-name) {:x-ahead-declaration declaration-id}])))
       (into {})))

(defn build-properties-ranges-map [declaration-syntax-rules vocabulary]
  (->> declaration-syntax-rules
       (mapv (fn [syntax-rule]
               [(keyword (domain/syntax-label syntax-rule))
                (common/find-vocabulary-property syntax-rule vocabulary)]))
       (filter (fn [[label prop]] (some? prop)))
       (mapv (fn [[label prop]] [label (domain/range prop)]))
       (into {})))

(defn fold-declarations [acc
                         vocabulary
                         {:keys [declaration-name declaration-node declaration-type]}
                         {:keys [parsed-location
                                 declaration-properties-ranges-map
                                 working-references] :as context}]
  (debug (str "Processing declaration " declaration-name))
  (let [location-meta        (meta declaration-node)
        declaration-node     (common/purge-ast declaration-node)
        declaration-node     (if (syntax/fragment? declaration-node)
                               ;; avoiding situations where we transform this into an include
                               ;; and then we cannot transform this back into declaration because there's
                               ;; no way to tell it without source maps
                               {:declaration declaration-node}
                               declaration-node)
        declaration-name     (url/url-encode (utils/safe-str declaration-name))
        declaration-id       (utils/path-join parsed-location declaration-name)
        type-hint            (get declaration-properties-ranges-map declaration-type)
        declaration-fragment (domain-parser/parse-ast vocabulary
                                                      declaration-node
                                                      (-> context
                                                          (assoc :references @working-references)
                                                          (assoc :parsed-location declaration-id)
                                                          (assoc :is-fragment false)
                                                          (assoc :type-hint type-hint)))
        sources              (or (-> declaration-fragment :sources) [])
        ;; we annotate the parsed declaration with the is-declaration source map so we can distinguish it from other declarations
        sources              (concat sources (common/generate-is-type-sources declaration-name
                                                                              (utils/path-join parsed-location declaration-name)
                                                                              declaration-id))
        parsed-declaration   (assoc declaration-fragment :sources sources)
        parsed-declaration   (if (nil? (:name parsed-declaration))
                               (assoc parsed-declaration :name declaration-name)
                               parsed-declaration)
        parsed-declaration   (assoc parsed-declaration :lexical location-meta)]
    ;; let's also update the working reference to this ahead declaration
    (swap! working-references (fn [old-working-references] (assoc old-working-references (keyword declaration-name) parsed-declaration)))
    (assoc acc (keyword (utils/alias-chain declaration-name context)) parsed-declaration)))


(defn process-declarations [vocabulary node {:keys [parsed-location references main-class] :as context}]
  (if (some? main-class)
    (let [syntax-rules (domain/syntax-rules main-class)
          declaration-syntax-rules (->> syntax-rules
                                        (filterv (fn [rule] (and (domain/declaration rule)
                                                                (common/declaration-property? rule vocabulary)))))

          declaration-properties (->> declaration-syntax-rules
                                      (map #(keyword (domain/syntax-label %))))

          declaration-properties-ranges-map (build-properties-ranges-map declaration-syntax-rules vocabulary)
          declarations (->> declaration-properties
                            (mapv (fn [declaration-type]
                                    (mapv (fn [[declaration-name declaration-node]]
                                            {:declaration-name declaration-name
                                             :declaration-node declaration-node
                                             :declaration-type declaration-type})
                                     (common/ast-get (syntax/<-data node) declaration-type)))))
          declarations (apply concat declarations)]
      (debug "Processing " (count declarations) " declarations")
      (let [;; we will mark the positions of references in the declarations node
            ahead-references (ahead-references declarations parsed-location)
            working-references (atom (merge references ahead-references))]
        (->> declarations
             (reduce (fn [acc next-declaration]
                       (fold-declarations acc vocabulary next-declaration
                                          (-> context
                                              (assoc :declaration-properties-ranges-map
                                                     declaration-properties-ranges-map)
                                              (assoc :working-references
                                                     working-references))))
                     {}))))
    {}))

(defn parse-document [vocabulary node context]
  (let [location (syntax/<-location node)
        context (assoc context :base-uri location)
        context (assoc context :parsed-location (str location "#/"))
        declarations (process-declarations vocabulary node context)
        context (assoc context :references declarations)]
    (-> (document/map->ParsedDocument
         {:id location
          :encodes (domain-parser/parse-ast vocabulary (syntax/<-data node) context)
          :declares (vals declarations)
          :document-type (str "#%" (domain/dialect vocabulary) " " (domain/vocabulary-version vocabulary))}))))

(defn parse-fragment [vocabulary node context]
  ;;(domain-parser/parse-ast vocabulary node context)
  (throw (new #?(:clj Exception :cljs js/Error) "meta-parsing of fragments not supported yet"))
  )

(defn parse-library [vocabulary node context]
  (throw (new #?(:clj Exception :cljs js/Error) "meta-parsing of libraries not supported yet"))
  )


(defn parse-vocabulary [fragment {:keys [vocabularies]}]
  (let [found-vocabulary (->> vocabularies
                              (filter (fn [vocabulary]
                                        (and (not (nil? (string/index-of fragment (domain/dialect vocabulary))))
                                             (not (nil? (string/index-of fragment (domain/vocabulary-version vocabulary)))))))
                              first)]
    (if (nil? found-vocabulary)
      (throw (new #?(:clj Exception :cljs js/Error)
                  (str "Cannot find vocabulary for fragment " fragment)))
      found-vocabulary)))

(defn parse-doc-type [vocabulary fragment]
  (let [fragment (string/replace fragment " " "")
        parts (string/split fragment (re-pattern (domain/vocabulary-version vocabulary)))
        suffix (if (= 2 (count parts))
                 (last parts)
                 nil)]
    (cond
      (nil? suffix)        :document
      (= suffix "Library") :library
      :else                :fragment)))


(defn parse-ast [node context]
  (cond
    (and (some? (syntax/<-location node))
         (some? (syntax/<-fragment node)))      (let [fragment (string/trim (syntax/<-fragment node))
                                                      vocabulary (parse-vocabulary fragment context)
                                                      main-class (->> (domain/classes vocabulary)
                                                                      (filter #(= (vocabulary/document-ns "RootDomainElement")
                                                                                  (-> [(document/extends %)] flatten first)))
                                                                      first)
                                                      context (-> context
                                                                  (assoc :main-class main-class)
                                                                  (assoc :type-hint (if (some? main-class) (document/id main-class) nil)))
                                                      doc-type (parse-doc-type vocabulary fragment)]
                                                  (condp = doc-type
                                                    :document (parse-document vocabulary node context)
                                                    :fragment (parse-fragment vocabulary node context)
                                                    :library (parse-library vocabulary node context)))

    (and (nil? (syntax/<-location node))
         (nil? (syntax/<-fragment node)))       (throw
                                                 (new #?(:clj Exception :cljs js/Error)
                                                      (str "Unsupported RAML parsing unit, missing @location or @fragment information")))

    :else                                               nil))
