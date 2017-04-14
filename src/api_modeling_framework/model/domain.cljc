(ns api-modeling-framework.model.domain
  (:require [api-modeling-framework.model.document :as document]))

(defprotocol DomainElement
  (abstract [this] "The node is partial description of the element"))

(defprotocol CommonAPIProperties
  (host [this] "Optional common host for all nodes in the API")
  (scheme [this] "Optional collection of schemes used by default in all the API endpoints")
  (accepts [this] "Optional list of accept media types supported by all endpoints in the API")
  (content-type [this] "Optional list of content media types supported by all endpoints in the API"))

(defprotocol HeadersHolder
  (headers [this] "List of HTTP headers exchanged by the API"))

(defprotocol ParametersHolder
  (parameters [this] "Parameters for this request"))

(defprotocol APIDocumentation
  (provider [this] "Person or organisation providing the API")
  (terms-of-service [this] "Terms of service for the API")
  (version [this] "Version of the API")
  (license [this] "License for the API")
  (base-path [this] "Optional base path for all API endpoints")
  (endpoints [this] "List of endpoints in the API"))

(defrecord ParsedAPIDocumentation [id abstract sources name description extends parameters additional-properties
                                   host scheme base-path accepts content-type headers
                                   provider terms-of-service version license endpoints]
  DomainElement
  (abstract [this] abstract)
  CommonAPIProperties
  (host [this] host)
  (scheme [this] scheme)
  (accepts [this] accepts)
  (content-type [this] content-type)
  HeadersHolder
  (headers [this] (or headers []))
  ParametersHolder
  (parameters [this] (or parameters []))
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
                      (some? (version this))))
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))

(defprotocol DomainPropertySchema
  (domain [this] "Elements in the domain of the property")
  (range [this] "Range of the property"))

(defrecord ParsedDomainPropertySchema [id name description sources additional-properties extends domain range]
  DomainPropertySchema
  (domain [this] domain)
  (range [this] (or range []))
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))

(defprotocol DomainProperty
  (object [this] "Value of the domain property"))

(defrecord ParsedDomainProperty [id name description sources extends object]
  DomainProperty
  (object [this] object)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] []))

(defprotocol EndPoint
  (supported-operations [this] "HTTP operations supported by this end-point")
  (path [this] "(partial) IRI template where the operations are bound to"))

(defrecord ParsedEndPoint [id abstract sources name description extends additional-properties path supported-operations parameters]
  DomainElement
  (abstract [this] abstract)
  EndPoint
  (supported-operations [this] supported-operations)
  (path [this] path)
  ParametersHolder
  (parameters [this] (or parameters []))
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))

(defprotocol Payload
  (schema [this] "Schema for the payload")
  (media-type [this] "Media type associated to this payload"))

(defrecord ParsedPayload [id abstract sources name description extends additional-properties
                          schema media-type]
  DomainElement
  (abstract [this] abstract)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this]
    ;; This should not be supported
    description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties []))
  Payload
  (schema [this] schema)
  (media-type [this] media-type))

(defprotocol PayloadHolder
  (payloads [this] "media-type / payload schemas"))

(defprotocol Operation
  (method [this] "HTTP method this operation is bound to")
  (request [this] "HTTP request information")
  (responses [this] "HTTP responses"))

(defrecord ParsedOperation [id abstract sources name description extends additional-properties
                            method host scheme accepts content-type
                            responses request]
  DomainElement
  (abstract [this] abstract)
  Operation
  (method [this] method)
  (request [this] request)
  (responses [this] responses)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties []))
  CommonAPIProperties
  (scheme [this] scheme)
  (accepts [this] accepts)
  (content-type [this] content-type))

(defprotocol Response
  (status-code [this] "Status code for the response"))


(defrecord ParsedResponse [id abstract sources name description extends additional-properties
                           status-code headers payloads]
  DomainElement
  (abstract [this] abstract)
  Response
  (status-code [this] status-code)
  PayloadHolder
  (payloads [this] payloads)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties []))
  HeadersHolder
  (headers [this] (or headers [])))


(defprotocol Type
  (shape [this] "Constraints for the data type"))


(defrecord ParsedType [id abstract sources name extends description shape additional-properties]
  DomainElement
  (abstract [this] abstract)
  Type
  (shape [this] shape)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))


(defprotocol Parameter
  (parameter-kind [this] "What kind of parameter is this")
  (required [this] "Is this parameter required"))

(defrecord ParsedParameter [id abstract sources name description extends additional-properties
                            parameter-kind shape required]
  DomainElement
  (abstract [this] abstract)
  Parameter
  (parameter-kind [this] parameter-kind)
  (required [this] required)
  Type
  (shape [this] shape)
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))

(defrecord ParsedRequest [id abstract sources name description extends additional-properties parameters accepts headers payloads]
  DomainElement
  (abstract [this] abstract)
  HeadersHolder
  (headers [this] (or headers []))
  ParametersHolder
  (parameters [this] (or parameters []))
  PayloadHolder
  (payloads [this] (or payloads []))
  document/Node
  (id [this] id)
  (name [this] name)
  (description [this]
    ;; This should not be supported
    description)
  (sources [this] sources)
  (valid? [this] true)
  (extends [this] (or extends []))
  (additional-properties [this] (or additional-properties [])))
