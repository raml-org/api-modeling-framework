IMPORTANT NOTES:
- 1/9/2018: This version has been deprecated. A new implementation is in the works which will replace this one. Stay tuned.
- 7/17/2018: Check-out the new implementation sources [here](https://github.com/aml-org/amf) and the project introduction [here](https://a.ml).

***

# API Modeling Framework

This project aims to provide a common programming interface that lets developers interact with any API specification, whether it is written in OpenAPI Specification (OAS) or RAML, in a similar way to how the HTML DOM allows programmatic interaction with an HTML document.

MuleSoft provides a [playground website](https://mulesoft-labs.github.io/amf-playground/) for everyone to preview and play with the API Modeling Framework and its capabilities like interoperability, introspection, and more.

## Vision

The API Modeling Framework (AMF) allows users to formally describe different kind of APIs, parse and generate instances of those APIS as sets of modular documents and to store those connected descriptions into a single unified data graph.

![Overview](https://raw.githubusercontent.com/raml-org/api-modeling-framework/master/images/diagram.png)

## Status

AMF is experimental and under active development.
No artifacts have been pushed yet to any repository but the current version of the library can be built from the source code in this project.
Changes to the current interfaces and vocabularies are to be expected, as well as a possible split of the library into smaller units.

## Goals

- Support for multiple languages with a unified output API/model for clients
- Support for both, document (RAML modularity) and domain (service clients), layers
- Bi-directional transformation
- Support for validation at document and service layers
- Produce a formal specification for the language
- Extensible, single document model for multiple domain vocabularies
- Consistent parsing behaviour

## Concepts

### Parsing and Generation

AMF can be used as a tool to parse RAML and OAS documents, generating a common data model stored in a data graph as the output.

![Translation](https://raw.githubusercontent.com/raml-org/api-modeling-framework/gh-pages/images/translation.png)

This model can be queried and manipulated, and then serialised back into documents using RAML or OAS syntaxes.
Additional syntaxes like JSON-LD are also supported by the library.

### Interoperability

API information in the AMF model can not only be parsed and generated from RAML and OAS documents, the library also provides functionality for pieces of API description expressed in different syntaxes to work together through the common AMF data model.

### Modularity and Reusability

AMF's data model is modelled after RAML modular features. API descriptions are not monolithic entities confined into a single document. Instead the RAML and AMF conception of APIs is that of a set of reusable behaviours, data types and practises that can be re-used and connected across different APIs in a organisation.

![Composition](https://raml-org.github.io/api-modeling-framework/images/composition.png)


AMF supports different kind of reusable units. The sum of all these units conform AMF's Document Model:

- *Documents*: main entry points for an API description, describing the top level API object
- *Fragments*: Small pieces of API description encapsulating a particular data type, or feature that can be re-used in other units
- *Modules*: Libraries of related elements from an API with identifiers that can referenced in other units

These units can be connected by relationships of inclusion and extension to build aggregate or adapt descriptions

![Document Model](https://raml-org.github.io/api-modeling-framework/images/document_model.png)

### Linked Descriptions

API modular units in the Document Model are connected through hyperlinks. The form a graph of documents that can be explored, easily versioned and exposed using standard HTTP technology. The Web is the native API for AMF.

### Extensible APIs

Units in the Document Model can encode descriptions of APIs for different domains as sets of extensible vocabularies.

RAML annotations and OAS patterned objects are examples of how extensibility is an important feature of an API model, no matter how complete it is, users will need to adapt and extend it.
In fact, RAML and OAS can be regarded as a collection of vocabularies to describe different domain: HTTP RPC APIs, authentication mechanisms and data shapes.

AMF boost these capacities through the notion of an [extensible Domain Model](https://raml-org.github.io/api-modeling-framework/vocabularies.html).

![Domain Model](https://raml-org.github.io/api-modeling-framework/images/domain_model.png)

New vocabularies can be defined and connected, at the same time, with the existing ones, re-using components already defined.
Eventually AMF will provide tools to easily define this extensions and generate parsers and generators for them.

### Unified Data Graph

Linked API descriptions in the Domain Model split into units in the Document Mode can be combined by the AMF into a single data graph using the resolution algorithm. Pieces of API description from different documents are put together into a single local graph that contains a functional description of the API, composed only of Domain Model elements.
The information into this local data graph can be used by clients, like HTTP clients that just need to consume a API endpoint without caring about the set of linked documents the description of that API is broken into.

### Unified Data Validation

AMF uses a formal data shape validation language in the model to provide validation services for the data conforming to the described APIs. RAML Types and JSON Schemas are translated into these formal constraint language and can be used to validate JSON-like data structures. Data constraints can also be exported as RAML Types or JSON Schema documents, as part of the syntax generation capabilities of the framework.

### Formal Definition and Standards

AMF provide a formal way of describing the vocabularies in the Domain Model, using W3C standards like OWL and RDF Schema. Using formal description tools allow us to reduce the ambiguity in the description of APIs and opens the door to interesting applications like logical inference.
Other W3C standards like RDF as the logical model for the hypermedia data graph of linked APIs or SHACL for data and constraint validation are also used. Using standards allows us not to reinvent the wheel but to build on top of well understood technologies with broad support across languages and platforms.


## Architecture

![Architectural diagram](doc/images/arch.png)

The current architecture relies on a collection of parsers and generators for RAML, OAS and JSON-LD (native RDF serialisation). These parsers follow the linked API descriptions in the document model and generate RDF graph data according to the semantics of the HTTP/RPC and data shapes vocabularies.

The resolution service can then be used to combine the information from all these documents in the graph into a single Domain Model resolved graph.
Any subset of the original or resolved graph can be exported back as RAML, OAS or JSON-LD syntaxes.


## Installation

The API Modeling Framework Github's repository includes can be used to build the following artifacts:

- API Modeling Framework Clojure/JVM JAR library
- API Modeling Framework Clojurescript/Node NPM package
- API Modeling Framework Clojurescript/Web library

The following leiningen invocations can be used to build each of this artifacts:


### API Modeling Framework Clojure/JVM library

``` shell
$ lein jar
```

### API Modeling Framework Clojurescript/Node library

``` shell
$ lein npm install # this is only required on the first run
$ lein node
```

The output NPM package will be generated at `output/node`.


### API Modeling Framework Clojurescript/Web library

``` shell
$ lein web
```

The output JS library will be generated at `output/web`.


## Tests

The project includes two different test suites, one for the Clojure/JVM code and another one for the Clojruescript/JS code.

Tests can be run using the following leiningen invocations:

### Clojure/JVM tests

``` shell
$ lein test
```

### Clojurescript/JS tests

``` shell
$ lein npm install # this is only required on the first run
$ lein test-js
```

## Vocabularies

The OWL ontology for the AMF vocabularies (document and domain models) [can be found here](https://github.com/raml-org/api-modeling-framework/blob/master/vocabulary/amf.ttl).
Reference documentation for the ontology can be found [here](https://raml-org.github.io/api-modeling-framework/vocabularies.html).

## Implementation Progress

The following tables show the current support of the project for different syntactical elements of RAML, OAS, RAML Types and JSONSchema.


**RAML Nodes**

| Node type | Status |
|----------|-----------|
| API  | partial, missing properties |
| DocumentationItem | missing |
| Resource | partial, missing properties |
| Method | partial, missing properties
| Response |complete |
| RequestBody | complete |
| ResponseBody | complete |
| Example | missing |
| ResourceType | missing |
| Trait | partial, missing properties |
| SecurityScheme | missing |
| SecuritySchemeSettings | missing |
| AnnotationType | coplete |
| Library | complete |
| Overlay | missing |
| Extension | missing |


**RAML Types**


| Node type | Status |
|----------|-----------|
| Any type | complete |
| Object type | partial, missing properties |
| Array type | complete |
| Scalar types | partial, missing properties |
| Union types | partial, missing properties |
| XML type | complete |
| JSON type | complete |


**Open API Nodes**

| Node type | Status |
|----------|-----------|
| Swagger | partial, missing properties |
| Info | partial, missing properties |
| Contact | missing |
| License | missing |
| Path Item | complete |
| Operation | partial, missing properties |
| External Documentation | missing |
| Parameter | partial, missing properties |
| Items | partial, missing properties |
| Responses | complete |
| Response | partial, missing properties |
| Headers | complete |
| Header | partial, missing properties |
| Tag | complete |
| XML | missing |


**JSON Schema**

| Node type | Status |
|----------|-----------|
| String type | partial, missing properties |
| Numeric types | partial, missing properties |
| Object type | partial, missing properties |
| Array type | complete |
| Boolean type | complete |
| Null type | complete |
| Schema combinations | partial, missing properties |


## TCK

As part of the development of the library, we are developing a cross-language version of the [RAML TCK](https://github.com/raml-org/raml-tck).
The list of test cases currently supporte can be found in the [tck](https://github.com/raml-org/api-modeling-framework/tree/master/resources/tck/raml-1.0) directory of the repository

## Contribution

If you are interested in contributing some code to this project, thanks! Please first [read and accept the Contributors Agreement](https://api-notebook.anypoint.mulesoft.com/notebooks#bc1cf75a0284268407e4).

To discuss this project, please use its [github issues](https://github.com/raml-org/api-modeling-framework/issues) or the [RAML forum](http://forums.raml.org/).

## License

Licensed under the Apache 2.0 License

Copyright 2017 MuleSoft.
