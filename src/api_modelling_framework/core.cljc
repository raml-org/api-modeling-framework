(ns api-modelling-framework.core
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  #?(:clj (:require [clojure.core.async :refer [<! >! go chan] :as async]
                    [api-modelling-framework.model.syntax :as syntax]
                    [api-modelling-framework.model.document :as document]
                    [api-modelling-framework.resolution :as resolution]
                    [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
                    [api-modelling-framework.parser.syntax.json :as json-parser]
                    [api-modelling-framework.parser.document.raml :as raml-document-parser]
                    [api-modelling-framework.parser.document.openapi :as openapi-document-parser]
                    [api-modelling-framework.generators.syntax.yaml :as yaml-generator]
                    [api-modelling-framework.generators.syntax.json :as json-generator]
                    [api-modelling-framework.generators.document.raml :as raml-document-generator]
                    [api-modelling-framework.generators.document.openapi :as openapi-document-generator]
                    [api-modelling-framework.generators.document.jsonld :as jsonld-document-generator]
                    [clojure.string :as string]
                    [api-modelling-framework.platform :as platform]
                    [clojure.walk :refer [keywordize-keys]]
                    [taoensso.timbre :as timbre :refer [debug]]))
  #?(:cljs (:require [cljs.core.async :refer [<! >! chan] :as async]
                     [api-modelling-framework.model.syntax :as syntax]
                     [api-modelling-framework.model.document :as document]
                     [api-modelling-framework.resolution :as resolution]
                     [api-modelling-framework.parser.syntax.yaml :as yaml-parser]
                     [api-modelling-framework.parser.syntax.json :as json-parser]
                     [api-modelling-framework.parser.document.raml :as raml-document-parser]
                     [api-modelling-framework.parser.document.openapi :as openapi-document-parser]
                     [api-modelling-framework.generators.syntax.yaml :as yaml-generator]
                     [api-modelling-framework.generators.syntax.json :as json-generator]
                     [api-modelling-framework.generators.document.raml :as raml-document-generator]
                     [api-modelling-framework.generators.document.openapi :as openapi-document-generator]
                     [api-modelling-framework.generators.document.jsonld :as jsonld-document-generator]
                     [api-modelling-framework.platform :as platform]
                     [clojure.walk :refer [keywordize-keys]]
                     [clojure.string :as string]
                     [taoensso.timbre :as timbre :refer-macros [debug]])))

(defn -registerInterface [] nil)

#?(:cljs (set! *main-cli-fn* -registerInterface))

#?(:cljs (defn ^:export fromClj [x] (clj->js x)))
#?(:cljs (defn ^:export toClj [x] (js->clj x)))

(defprotocol Model
  (^:export location [this] "Location of the model if any")
  (^:export document-model [this] "returns the domain model for the parsed document")
  (^:export domain-model [this] "Resolves the document model generating a domain model")
  (^:export reference-model [this location] "Returns a model for a nested reference ")
  (^:eport find-element [this level id] "Finds a domain element in the model data, returning the element wrapped in a fragment"))

(defprotocol Parser
  (^:export parse-file [this uri cb]
   "Parses a local or remote stand-alone document file and builds a model")
  (^:export parse-string [this uri string cb]
   "Parses a raw string with document URI identifier and builds a model"))

(defprotocol Generator
  (^:export generate-string [this uri model options cb]
   "Serialises a model into a string")
  (^:export generate-file [this uri model options cb]
   "Serialises a model into a file located at the provided URI"))

(defn *find-element [model id]
  (cond
    (map? model) (if (= id (:id model))
                   model
                   (->> (vals model)
                        (map (fn [m] (*find-element m id)))
                        (filter some?)
                        first))
    (coll? model) (->> model
                       (map (fn [m] (*find-element m id)))
                       (filter some?)
                       first)
    :else         nil))

(defn to-model
  ([res]
   (let [domain-cache (atom nil)]
     (reify Model
       (location [_] (document/location res))
       (document-model [_] res)
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
               model (*find-element model id)]
           (if (some? model)
             (to-model (document/map->ParsedDocument {:location id
                                                      :encodes model
                                                      :resolved (= model "domain")
                                                      :references (:references res)
                                                      :declares (:declares res)}))
             nil)))))))

(defrecord RAMLParser []
  Parser
  (parse-file [this uri cb]
    (go (let [res (<! (yaml-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string [this uri string cb]
    (go (let [res (<! (yaml-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (raml-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

(defrecord OpenAPIParser []
  Parser
  (parse-file [this uri cb]
    (go (let [res (<! (json-parser/parse-file uri))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil)))))))
  (parse-string [this uri string cb]
    (go (let [res (<! (json-parser/parse-string uri string))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (try (cb nil (to-model (openapi-document-parser/parse-ast res {})))
                 (catch #?(:clj Exception :cljs js/Error) ex
                   (cb (platform/<-clj ex) nil))))))))

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

(defrecord APIModelGenerator []
  Generator
  (generate-string [this uri model options cb]
    (debug "Generating OpenAPI string")
    (go (try (let [options (keywordize-keys options)
                   res (-> model
                           (pre-process-model)
                           (jsonld-document-generator/to-jsonld (get options :source-maps? false))
                           (json-generator/generate-string options))]
               (cb nil (platform/<-clj res)))
             (catch #?(:clj Exception :cljs js/Error) ex
               (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (debug "Generating OpenAPI file")
    (go (let [options (keywordize-keys options)
              res (-> model
                      (pre-process-model)
                      (jsonld-document-generator/to-jsonld (get options :source-maps? false))
                      (json-generator/generate-string options))]
          (if (platform/error? res)
            (cb (platform/<-clj res) nil)
            (cb nil (platform/<-clj res)))))))

(defrecord RAMLGenerator []
  Generator
  (generate-string [this uri model options cb]
    (debug "Generating RAML string")
    (go (try (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                                   {:location uri}))
                   res (-> model
                           (pre-process-model)
                           (raml-document-generator/to-raml options)
                           (syntax/<-data))
                   res (yaml-generator/generate-string res options)]
              (cb nil (platform/<-clj res)))
            (catch #?(:clj Exception :cljs js/Error) ex
              (cb (platform/<-clj ex) nil)))))
  (generate-file [this uri model options cb]
    (debug "Generating RAML file")
    (go (let [options (keywordize-keys (merge (or (platform/->clj options) {})
                                              {:location uri}))
              res (<! (-> model
                          (pre-process-model)
                          (raml-document-generator/to-raml options)
                          (syntax/<-data)
                          (yaml-generator/generate-file uri options)))]
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
