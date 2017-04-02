(ns api-modelling-framework.core
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  #?(:clj (:require [clojure.core.async :refer [<! >! go chan] :as async]
                    [api-modelling-framework.model.syntax :as syntax]
                    [api-modelling-framework.model.document :as document]
                    [api-modelling-framework.resolution :as resolution]
                    [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
                    [api-modelling-framework.parser.syntax.json :as json-parser]
                    [api-modelling-framework.parser.syntax.jsonld :as jsonld-parser]
                    [api-modelling-framework.parser.document.raml :as raml-document-parser]
                    [api-modelling-framework.parser.document.openapi :as openapi-document-parser]
                    [api-modelling-framework.parser.document.jsonld :as jsonld-document-parser]
                    [api-modelling-framework.generators.syntax.yaml :as yaml-generator]
                    [api-modelling-framework.generators.syntax.json :as json-generator]
                    [api-modelling-framework.generators.syntax.jsonld :as jsonld-generator]
                    [api-modelling-framework.generators.document.raml :as raml-document-generator]
                    [api-modelling-framework.generators.document.openapi :as openapi-document-generator]
                    [api-modelling-framework.generators.document.jsonld :as jsonld-document-generator]
                    [clojure.string :as string]
                    [api-modelling-framework.platform :as platform]
                    [clojure.walk :refer [keywordize-keys stringify-keys]]
                    [taoensso.timbre :as timbre :refer [debug]]))
  #?(:cljs (:require [cljs.core.async :refer [<! >! chan] :as async]
                     [api-modelling-framework.model.syntax :as syntax]
                     [api-modelling-framework.model.document :as document]
                     [api-modelling-framework.resolution :as resolution]
                     [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
                     [api-modelling-framework.parser.syntax.json :as json-parser]
                     [api-modelling-framework.parser.syntax.jsonld :as jsonld-parser]
                     [api-modelling-framework.parser.document.raml :as raml-document-parser]
                     [api-modelling-framework.parser.document.openapi :as openapi-document-parser]
                     [api-modelling-framework.parser.document.jsonld :as jsonld-document-parser]
                     [api-modelling-framework.generators.syntax.yaml :as yaml-generator]
                     [api-modelling-framework.generators.syntax.json :as json-generator]
                     [api-modelling-framework.generators.syntax.jsonld :as jsonld-generator]
                     [api-modelling-framework.generators.document.raml :as raml-document-generator]
                     [api-modelling-framework.generators.document.openapi :as openapi-document-generator]
                     [api-modelling-framework.generators.document.jsonld :as jsonld-document-generator]
                     [api-modelling-framework.platform :as platform]
                     [clojure.walk :refer [keywordize-keys stringify-keys]]
                     [clojure.string :as string]
                     [taoensso.timbre :as timbre :refer-macros [debug]])))

(defn -registerInterface [] nil)

#?(:cljs (set! *main-cli-fn* -registerInterface))

#?(:cljs (defn ^:export fromClj [x] (clj->js x)))
#?(:cljs (defn ^:export toClj [x] (js->clj x)))

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
  (^:export location [this] "Location of the model if any")
  (^:export document-model [this] "returns the domain model for the parsed document")
  (^:export domain-model [this] "Resolves the document model generating a domain model")
  (^:export reference-model [this location] "Returns a model for a nested reference ")
  (^:export update-reference-model [this location syntax-type text cb] "Updates a model for a reference model")
  (^:eport find-element [this level id] "Finds a domain element in the model data, returning the element wrapped in a fragment")
  (^:export raw [this] "Returns the raw text for the model"))

(defprotocol Parser
  (^:export parse-file
   [this uri cb]
   [this uri options cb]
   "Parses a local or remote stand-alone document file and builds a model")
  (^:export parse-string
   [this uri string cb]
   [this uri string options cb]
   "Parses a raw string with document URI identifier and builds a model"))

(defprotocol Generator
  (^:export generate-string [this uri model options cb]
   "Serialises a model into a string")
  (^:export generate-file [this uri model options cb]
   "Serialises a model into a file located at the provided URI"))

(defrecord RAMLParser []
  Parser
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (go (let [res (<! (yaml-parser/parse-file uri options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (go (let [res (<! (yaml-parser/parse-string uri string options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord OpenAPIParser []
  Parser
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (go (let [res (<! (json-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (go (let [res (<! (json-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord APIModelParser []
  Parser
  (parse-file [this uri cb] (parse-file this uri {} cb))
  (parse-file [this uri options cb]
    (debug "Parsing APIModel file")
    (go (let [res (<! (jsonld-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (jsonld-document-parser/from-jsonld (stringify-keys (get res "@data")))))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string [this uri string cb] (parse-string this uri string {} cb))
  (parse-string [this uri string options cb]
    (debug "Parsing APIModel string")
    (go (let [res (<! (jsonld-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (jsonld-document-parser/from-jsonld (get res "@data"))))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord APIModelGenerator []
  Generator
  (generate-string [this uri model options cb]
    (debug "Generating APIModel string")
    (go (try (let [options (keywordize-keys options)
                   res (-> model
                           (pre-process-model)
                           (jsonld-document-generator/to-jsonld (get options :source-maps? (get options "source-maps?" false)))
                           (jsonld-generator/generate-string (get options :full-graph? (get options "full-graph?" true))))]
               (cb nil (platform/<-clj res)))
             (catch #?(:clj Exception :cljs js/Error) ex
               (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (debug "Generating APIModel file")
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
        (= fragment :fragment) "#% RAML 1.0"
        :else                  "#% RAML 1.0"))

(defrecord RAMLGenerator []
  Generator
  (generate-string [this uri model options cb]
    (debug "Generating RAML string")
    (go (try (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                                   {:location uri}))
                   res (-> model
                           (pre-process-model)
                           (raml-document-generator/to-raml options))
                   res (yaml-generator/generate-string (syntax/<-data res)
                                                       (assoc options :header (to-raml-fragment-header (syntax/<-fragment res))))]
               (cb nil (platform/<-clj res)))
            (catch #?(:clj Exception :cljs js/Error) ex
              (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (debug "Generating RAML file")
    (go (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                              {:location uri}))
              res (<! (-> model
                          (pre-process-model)
                          (raml-document-generator/to-raml options)))
              res (yaml-generator/generate-file (syntax/<-data res) uri (assoc options :header (to-raml-fragment-header (syntax/<-fragment res))))]
         (if (platform/error? res)
           (cb (platform/<-clj res) nil)
           (cb nil (platform/<-clj res)))))))


(defrecord OpenAPIGenerator []
  Generator
  (generate-string [this uri model options cb]
    (debug "Generating OpenAPI string")
    (go (try (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                                   {:location uri}))
                   res (-> model
                           (pre-process-model)
                           (openapi-document-generator/to-openapi options)
                           (syntax/<-data)
                           (json-generator/generate-string options))]
               (cb nil (platform/<-clj res)))
             (catch #?(:clj Exception :cljs js/Error) ex
               (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (debug "Generating OpenAPI file")
    (go (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                              {:location uri}))
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
    (map? model) (if (= id (:id model))
                   model
                   (->> (vals model)
                        (map (fn [m] (find-element* m id)))
                        (filter some?)
                        first))
    (coll? model) (->> model
                       (map (fn [m] (find-element* m id)))
                       (filter some?)
                       first)
    :else         nil))

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

     (let [domain-cache (atom nil)]
       (reify Model
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
                              (str "Unsupported spec " syntax-type " only 'ram' and 'openapi' supported"))}
                   nil))))

         (domain-model [_]
           (if (some? @domain-cache)
             @domain-cache
             (let [res (resolution/resolve res {})]
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
         (raw [this] (:raw res)))))))
