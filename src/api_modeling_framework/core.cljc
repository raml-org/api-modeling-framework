(ns api-modeling-framework.core
  (:require [clojure.core.async :refer [<! >! <!! go chan] :as async]
            [api-modeling-framework.model.syntax :as syntax]
            [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]
            [api-modeling-framework.model.vocabulary :as vocabulary]
            [api-modeling-framework.data :as data]
            [api-modeling-framework.resolution :as resolution]
            [api-modeling-framework.parser.syntax.yaml :as yaml-parser]
            [api-modeling-framework.parser.syntax.json :as json-parser]
            [api-modeling-framework.parser.syntax.jsonld :as jsonld-parser]
            [api-modeling-framework.parser.document.meta :as meta-document-parser]
            [api-modeling-framework.parser.document.raml :as raml-document-parser]
            [api-modeling-framework.parser.document.openapi :as openapi-document-parser]
            [api-modeling-framework.parser.document.jsonld :as jsonld-document-parser]
            [api-modeling-framework.generators.syntax.yaml :as yaml-generator]
            [api-modeling-framework.generators.syntax.json :as json-generator]
            [api-modeling-framework.generators.syntax.jsonld :as jsonld-generator]
            [api-modeling-framework.generators.document.raml :as raml-document-generator]
            [api-modeling-framework.generators.document.openapi :as openapi-document-generator]
            [api-modeling-framework.generators.document.jsonld :as jsonld-document-generator]
            [api-modeling-framework.utils :as utils]
            [clojure.string :as string]
            [api-modeling-framework.platform :as platform]
            [clojure.walk :refer [keywordize-keys stringify-keys]]))

(defn -registerInterface [] nil)

(declare to-model)

(defn pre-process-model
  "Prepares the model to be processed as a RAML document if the model has been resolved"
  [model]
  (if (document/resolved model)
    (-> model
        (document/remove-tag document/uses-library-tag)
        (assoc :resolved nil)
        (assoc :references nil)
        (assoc :uses nil)
        (assoc :declares nil))
    model))

(defprotocol Model
  (^:export unit-kind [this] "Kind of unit in the AMF model")
  (^:export location [this] "Location of the model if any")
  (^:export document-model [this] "returns the domain model for the parsed document")
  (^:export domain-model [this] "Resolves the document model generating a domain model")
  (^:export reference-model [this location] "Returns a model for a nested reference ")
  (^:export update-reference-model [this location syntax-type text cb] "Updates a model for a reference model")
  (^:export references [this] "Returns a list of all the files referenced by this model")
  (^:export find-element [this level id] "Finds a domain element in the model data, returning the element wrapped in a fragment")
  (^:export raw [this] "Returns the raw text for the model")
  (^:export lexical-info-for-unit [model unit-id] "Finds lexical information for a particular unit for a particular syntax (\"raml\", \"openapi\")"))

(defprotocol DataProtocol
  (^:export validate [this schema schema-type payload cb]))

(defrecord ^:export DataValidator []
  DataProtocol
  (validate [this schema schema-type payload cb]
    (go (let [res (<! (data/validate schema schema-type payload))]
          (if (some? (:err res))
            (cb (new #?(:clj Exception :cljs js/Error) (platform/<-clj (:err res))) nil)
            (cb nil (platform/<-clj res)))))))

(defprotocol Parser
  (^:export parse-file-sync
    [this uri]
    [this uri options])
  (^:export parse-file
   [this uri cb]
   [this uri options cb]
   "Parses a local or remote stand-alone document file and builds a model")
  (^:export parse-string
   [this uri string cb]
   [this uri string options cb]
   "Parses a raw string with document URI identifier and builds a model")
  (^:export parse-string-sync
   [this uri string]
   [this uri string options]))

(defprotocol Generator
  (^:export generate-string [this uri model options cb]
   "Serialises a model into a string")
  (^:export generate-file [this uri model options cb]
   "Serialises a model into a file located at the provided URI")
  (generate-string-sync [this uri model options])
  (generate-file-sync [this uri model options]))

(defn cb->sync [f]
  (<!! (let [c (chan)]
         (f (fn [e res]
              (go (if (some? e)
                    (>! c e)
                    (>! c res)))))
         c)))

(defrecord ^:export RAMLParser []
  Parser
  (parse-file-sync [this uri]
    (cb->sync (partial parse-file this uri)))
  (parse-file-sync [this uri options]
    (cb->sync (partial parse-file this uri options)))
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (raml-document-parser/reset-cache)
    (go (let [res (<! (yaml-parser/parse-file uri options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string-sync [this uri string]
    (cb->sync (partial parse-string this uri string)))
  (parse-string-sync [this uri string options]
    (cb->sync (partial parse-string this uri string options)))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (raml-document-parser/reset-cache)
    (go (let [res (<! (yaml-parser/parse-string uri string options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))


(defrecord ^:export OpenAPIParser []
  Parser
  (parse-file-sync [this uri]
    (cb->sync (partial parse-file this uri)))
  (parse-file-sync [this uri options]
    (cb->sync (partial parse-file this uri options)))
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (go (let [res (<! (json-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string-sync [this uri string]
    (cb->sync (partial parse-string this uri string)))
  (parse-string-sync [this uri string options]
    (cb->sync (partial parse-string this uri string options)))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (go (let [res (<! (json-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord ^:export APIModelParser []
  Parser
  (parse-file-sync [this uri]
    (cb->sync (partial parse-file this uri)))
  (parse-file-sync [this uri options]
    (cb->sync (partial parse-file this uri options)))
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (go (let [res (<! (jsonld-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (jsonld-document-parser/from-jsonld res)))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string-sync [this uri string]
    (cb->sync (partial parse-string this uri string)))
  (parse-string-sync [this uri string options]
    (cb->sync (partial parse-string this uri string options)))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (utils/debug "Parsing APIModel string")
    (go (let [res (<! (jsonld-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (jsonld-document-parser/from-jsonld res)))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defn parse-vocabulary [vocabulary-path context]
  (go (let [c (chan)
            parser (->RAMLParser)
            model (<! (utils/cb->chan (partial parse-file parser vocabulary-path)))
            vocabulary-model (document-model model)]
        (if (satisfies? document/Vocabulary vocabulary-model)
          (reduce (fn [vocabulary-model reference]
                    (if (satisfies? document/Vocabulary reference)
                      (let [reference (document/vocabulary reference)
                            properties (domain/properties reference)
                            classes (->> (domain/classes reference)
                                         (filter (fn [class-term]
                                                   (not= (vocabulary/document-ns "RootDomainElement")
                                                         (first (flatten [(document/extends class-term)]))))))
                            old-classes (domain/classes vocabulary-model)
                            old-properties (domain/properties vocabulary-model)]
                        (-> vocabulary-model
                            (assoc :classes (concat old-classes classes))
                            (assoc :properties (concat old-properties properties))))
                      vocabulary-model))
                  (document/vocabulary vocabulary-model)
                  (document/references vocabulary-model))
          nil))))

(defn parse-vocabularies [{:keys [vocabularies] :as context}]
  (go (let [vocabularies (or vocabularies [])]
        (utils/debug "Parsing " (count vocabularies) " vocabularies")
        (loop [parsed-vocabularies []
               vocabularies (or vocabularies [])]
          (if (empty? vocabularies)
            parsed-vocabularies
            (recur (let [parsed-vocabulary (<! (parse-vocabulary (first vocabularies) context))]
                     (if (nil? parsed-vocabulary)
                       parsed-vocabularies
                       (conj parsed-vocabularies parsed-vocabulary)))
                   (rest vocabularies)))))))

(defrecord ^:export MetaParser []
  Parser
  (parse-file-sync [this uri]
    (cb->sync (partial parse-file this uri)))
  (parse-file-sync [this uri options]
    (cb->sync (partial parse-file this uri options)))
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (utils/debug "Parsing Model file with vocabularies info")
    (go (let [vocabularies (<! (parse-vocabularies options))
              res (<! (yaml-parser/parse-file uri options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (meta-document-parser/parse-ast res (assoc options :vocabularies vocabularies))))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string-sync [this uri string]
    (cb->sync (partial parse-string this uri string)))
  (parse-string-sync [this uri string options]
    (cb->sync (partial parse-string this uri string options)))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (utils/debug "Parsing Model file from string with vocabularies info")
    (go (let [vocabularies (<! (parse-vocabularies options))
              res (<! (yaml-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (meta-document-parser/parse-ast res (assoc options :vocabularies vocabularies))))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord ^:export APIModelGenerator []
  Generator
  (generate-string-sync [this uri model options] (cb->sync (partial generate-string this uri model options)))
  (generate-file-sync [this uri model options] (cb->sync (partial generate-file this uri model options)))
  (generate-string [this uri model options cb]
    (utils/debug "Generating APIModel string")
    (go (try (let [options (keywordize-keys options)
                   res (-> model
                           (pre-process-model)
                           (jsonld-document-generator/to-jsonld (get options :source-maps? (get options "source-maps?" false)))
                           (jsonld-generator/generate-string (get options :full-graph? (get options "full-graph?" true))))]
               (cb nil (platform/<-clj res)))
             (catch #?(:clj Exception :cljs js/Error) ex
               (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (utils/debug "Generating APIModel file")
    (go (let [options (keywordize-keys options)
              res (-> model
                      (pre-process-model)
                      (jsonld-document-generator/to-jsonld (get options :source-maps? (get options "source-maps?" false)))
                      (jsonld-generator/generate-string (get options :full-graph? (get options "full-graph?" true))))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (cb nil (platform/<-clj res)))))))

(defn to-raml-fragment-header [fragment]
  (cond (string? fragment) fragment
        (= fragment :fragment) "#%RAML 1.0"
        :else                  "#%RAML 1.0"))

(defrecord ^:export RAMLGenerator []
  Generator
  (generate-string-sync [this uri model options] (cb->sync (partial generate-string this uri model options)))
  (generate-file-sync [this uri model options] (cb->sync (partial generate-file this uri model options)))
  (generate-string [this uri model options cb]
    (utils/debug "Generating RAML string")
    (go (try (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                                   {:location uri
                                                    :syntax :raml}))
                   res (-> model
                           (pre-process-model)
                           (raml-document-generator/to-raml options))
                   res (yaml-generator/generate-string (syntax/<-data res)
                                                       (assoc options :header (to-raml-fragment-header (syntax/<-fragment res))))]
               (cb nil (platform/<-clj res)))
            (catch #?(:clj Exception :cljs js/Error) ex
              (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (utils/debug "Generating RAML file")
    (go (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                              {:location uri
                                               :syntax :raml}))
              res (<! (-> model
                          (pre-process-model)
                          (raml-document-generator/to-raml options)))
              res (yaml-generator/generate-file (syntax/<-data res) uri (assoc options :header (to-raml-fragment-header (syntax/<-fragment res))))]
         (if (platform/error? res)
           (cb (platform/<-clj res) nil)
           (cb nil (platform/<-clj res)))))))


(defrecord ^:export OpenAPIGenerator []
  Generator
  (generate-string-sync [this uri model options] (cb->sync (partial generate-string this uri model options)))
  (generate-file-sync [this uri model options] (cb->sync (partial generate-file this uri model options)))
  (generate-string [this uri model options cb]
    (utils/debug "Generating OpenAPI string")
    (go (try (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                                   {:location uri
                                                    :syntax :openapi}))
                   res (-> model
                           (pre-process-model)
                           (openapi-document-generator/to-openapi options)
                           (syntax/<-data)
                           (json-generator/generate-string options))]
               (cb nil (platform/<-clj res)))
             (catch #?(:clj Exception :cljs js/Error) ex
               (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (utils/debug "Generating OpenAPI file")
    (go (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                              {:location uri
                                               :syntax :openapi}))
              res (-> model
                      (pre-process-model)
                      (openapi-document-generator/to-openapi options)
                      (syntax/<-data)
                      (json-generator/generate-string options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (cb nil (platform/<-clj res)))))))

(defn find-element* [model id]
  (cond
    ;; we prefer elements in declarations if present
    (and (map? model)
         (:declares model)
         (some #(= id (:id %))
               (:declares model)))  (->> (:declares model)
                                       (filter #(= id (:id %)))
                                       first)

    (map? model)                     (if (= id (or (:id model) (get model "@id")))
                                       model
                                       (->> (vals model)
                                            (map (fn [m] (find-element* m id)))
                                            (filter some?)
                                            first))

    (coll? model)                     (->> model
                                           (map (fn [m] (find-element* m id)))
                                           (filter some?)
                                           first)

    :else                             nil))

(defn to-model
  ([res]

   (letfn [(update-reference-model* [old-model updated-unit]
             (let [new-references (->> (document/references old-model)
                                       (filter #(not= (document/location %) (document/location updated-unit))))
                   new-references (concat new-references [(document-model updated-unit)])
                   updated-model (assoc old-model :references new-references)]
               (to-model updated-model)))

           (parsing-channel [syntax-type location text]
             (let [ch (chan)]
               (condp = syntax-type
                 "raml" (parse-string (RAMLParser.) location text {} (fn [e r] (go (>! ch r))))
                 "open-api" (parse-string (OpenAPIParser.) location text {} (fn [e r] (go (>! ch r)))))
               ch))]

     (let [domain-cache (atom nil)
           lexical-cache-raml (atom {})]
       (reify Model
         (unit-kind [_]
           (cond
             (and (satisfies? document/Module res)
                  (satisfies? document/Fragment res)) "document"
             (satisfies? document/Module res)         "module"
             (satisfies? document/Fragment res)       "fragment"
             :else                                    "unit"))
         (location [_] (document/location res))

         (document-model [_] res)

         (update-reference-model [this location syntax-type text cb]
           (go
             (if (or (= syntax-type "raml") (= syntax-type "open-api"))
               (let [parsed-unit (<! (parsing-channel syntax-type location text))]
                 (if (= (document/location res) location)
                   (cb nil parsed-unit)
                   (let []
                     (cb nil (update-reference-model* res parsed-unit)))))
               (cb {:err (new #?(:clj Exception :cljs js/Error)
                              (str "Unsupported spec " syntax-type " only 'raml' and 'openapi' supported"))}
                   nil))))

         (domain-model [_]
           (if (some? @domain-cache)
             @domain-cache
             (let [res (resolution/resolve-domain-element res {})]
               (reset! domain-cache res)
               res)))

         (reference-model [this location]
           (if (= location (document/location res))
             this
             (let [reference (->> (document/references res)
                                  (filter #(= location (document/location %)))
                                  first)]
               (if (some? reference)
                 (to-model reference)
                 (throw (new #?(:clj Exception :cljs js/Error) (str "Cannot find reference " location " in the model")))))))

         (find-element [this level id]
           (let [model (if (= level "document")
                         res
                         (domain-model this))
                 model (find-element* model id)]
             (if (some? model)
               (to-model (document/map->ParsedDocument {:location id
                                                        :encodes model
                                                        :resolved (= model "domain")
                                                        :references (:references res)
                                                        :declares (:declares res)}))
               nil)))

         (references [this]
           (-> (->> res
                    document/references
                    (map :location))
               platform/<-clj))

         (lexical-info-for-unit [this unit-id]
           (let [cache lexical-cache-raml]
             (platform/<-clj
              (if-let [lexical-info (get @cache unit-id)]
                lexical-info
                (let [element (find-element* res unit-id)
                      lexical-info (:lexical element)]
                  (if (some? lexical-info)
                    (do
                      (swap! cache (fn [acc] (assoc acc unit-id lexical-info)))
                      lexical-info)
                    nil))))))

         (raw [this]
           (:raw res)))))))
