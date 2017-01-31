(ns raml-framework.model.domain
  (:require [raml-framework.model.document :as document]))

(defprotocol CommonAPIProperties
  (host [this] "Optional common host for all nodes in the API")
  (scheme [this] "Optional collection of schemes used by default in all the API endpoints")
  (accepts [this] "Optional list of accept media types supported by all endpoints in the API")
  (content-type [this] "Optional list of content media types supported by all endpoints in the API")
  (headers [this] "List of HTTP headers exchanged by the API"))

(defprotocol APIDocumentation
  (provider [this] "Person or organisation providing the API")
  (terms-of-service [this] "Terms of service for the API")
  (version [this] "Version of the API")
  (license [this] "License for the API")
  (base-path [this] "Optional base path for all API endpoints")
  (endpoints [this] "List of endpoints in the API"))

(defrecord ParsedAPIDocumentation [id sources name description host scheme base-path accepts content-type
                                   provider terms-of-service version license endpoints]
  CommonAPIProperties
  (host [this] host)
  (scheme [this] scheme)
  (accepts [this] accepts)
  (content-type [this] content-type)
  (headers [this] [])
  APIDocumentation
  (provider [this] provider)
  (terms-of-service [this] terms-of-service)
  (version [this] version)
  (license [this] license)
  (base-path [this] base-path)
  (endpoints [this] endpoints)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] (and (some? (name this))
                      (some? (version this)))))


(defprotocol DomainElement
  (fragment-node [this] "The kind of node this domain element is wrapping")
  (properties [this] "A map of properties that can be used to build a concrete domain component")
  (to-domain-node [this] "Transforms this partially parsed domain element into a concrete domain component"))

(defrecord  ParsedDomainElement [id fragment-node properties]
  document/Node
  (id [this] id)
  (name [this] "Domain element [" fragment-node "]")
  (description [this] (str "Partially parsed node with properties of type  " fragment-node))
  (sources [this] (:sources properties))
  (valid? [this] true)
  DomainElement
  (fragment-node [this] fragment-node)
  (properties [this] properties)
  (to-domain-node [this]
    (condp = fragment-node
      :parsed-api-documentation (map->ParsedAPIDocumentation properties))))


(defprotocol EndPoint
  (supported-operations [this] "HTTP operations supported by this end-point")
  (path [this] "(partial) IRI template where the operations are bound to"))

(defrecord ParsedEndPoint [id sources name description path supported-operations]
  EndPoint
  (supported-operations [this] supported-operations)
  (path [this] path)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true))

(defprotocol Operation
  (method [this] "HTTP method this operation is bound to"))


(defrecord ParsedOperation [id sources name description method headers host scheme accepts content-type]
  Operation
  (method [this] method)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  CommonAPIProperties
  (host [this] host)
  (scheme [this] scheme)
  (accepts [this] accepts)
  (content-type [this] content-type)
  (headers [this] headers))
