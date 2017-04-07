"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// Namespaces
exports.HYDRA_NS = "http://www.w3.org/ns/hydra/core#";
exports.DOCUMENT_NS = "http://raml.org/vocabularies/document#";
exports.HTTP_NS = "http://raml.org/vocabularies/http#";
exports.SHAPES_NS = "http://raml.org/vocabularies/shapes#";
exports.RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
exports.RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
exports.SHACL_NS = "http://www.w3.org/ns/shacl#";
exports.XSD_NS = "http://www.w3.org/2001/XMLSchema#";
exports.SCHEMA_ORG_NS = "http://schema.org/";
exports.NS_MAPPING = {};
exports.NS_MAPPING[exports.HYDRA_NS] = "hydra";
exports.NS_MAPPING[exports.DOCUMENT_NS] = "doc";
exports.NS_MAPPING[exports.HTTP_NS] = "http";
exports.NS_MAPPING[exports.SHAPES_NS] = "shapes";
exports.NS_MAPPING[exports.SHACL_NS] = "shacl";
exports.NS_MAPPING[exports.RDF_NS] = "rdf";
exports.NS_MAPPING[exports.RDFS_NS] = "rdfs";
exports.NS_MAPPING[exports.XSD_NS] = "xsd";
exports.NS_MAPPING[exports.SCHEMA_ORG_NS] = "schema-org";
// RDF Classes
exports.DOMAIN_ELEMENT = exports.DOCUMENT_NS + "DomainElement";
exports.API_DOCUMENTATION = exports.HTTP_NS + "APIDocumentation";
exports.END_POINT = exports.HTTP_NS + "EndPoint";
exports.OPERATION = exports.HYDRA_NS + "Operation";
exports.RESPONSE = exports.HTTP_NS + "Response";
exports.REQUEST = exports.HTTP_NS + "Request";
exports.PAYLOAD = exports.HTTP_NS + "Payload";
exports.SCHEMA = exports.HTTP_NS + "Schema";
exports.INCLUDE_RELATIONSHIP = exports.DOCUMENT_NS + "IncludeRelationship";
// RDF Properties
exports.LABEL = exports.DOCUMENT_NS + "label";
exports.NAME = exports.SCHEMA_ORG_NS + "name";
exports.ENCODES = exports.DOCUMENT_NS + "encodes";
exports.ENDPOINT = exports.HTTP_NS + "endpoint";
exports.PATH = exports.HTTP_NS + "path";
exports.SUPPORTED_OPERATION = exports.HYDRA_NS + "supportedOperation";
exports.METHOD = exports.HYDRA_NS + "method";
exports.RETURNS = exports.HYDRA_NS + "returns";
exports.EXPECTS = exports.HYDRA_NS + "expects";
exports.STATUS_CODE = exports.HYDRA_NS + "statusCode";
exports.RESPONSE_PAYLOAD = exports.HTTP_NS + "payload";
exports.MEDIA_TYPE = exports.HTTP_NS + "mediaType";
exports.SCHEMA_SHAPE = exports.HTTP_NS + "shape";
exports.PAYLOAD_SCHEMA = exports.HTTP_NS + "schema";
exports.TARGET = exports.DOCUMENT_NS + "target";
// Utility functions
function extract_link(node, property) {
    return ((node[property] || [])[0]) || {};
}
function extract_links(node, property) {
    return (node[property] || []);
}
function extract_value(node, property) {
    const value = ((node[property] || [])[0]) || {};
    return value["@value"];
}
function has_type(node, type) {
    const types = node["@type"] || [];
    console.log("CHECKING TYPES");
    console.log(JSON.stringify(types));
    return types.find(t => t === type) != null;
}
class DomainElement {
    constructor(raw, id, label) {
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.kind = "DomainElement";
    }
}
exports.DomainElement = DomainElement;
class APIDocumentation extends DomainElement {
    constructor(raw, id, label, endpoints) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.endpoints = endpoints;
        this.kind = "APIDocumentation";
    }
}
exports.APIDocumentation = APIDocumentation;
class EndPoint extends DomainElement {
    constructor(raw, id, label, path, operations) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.path = path;
        this.operations = operations;
        this.kind = "EndPoint";
    }
}
exports.EndPoint = EndPoint;
class Operation extends DomainElement {
    constructor(raw, id, label, method, requests, responses) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.method = method;
        this.requests = requests;
        this.responses = responses;
        this.kind = "Operation";
    }
}
exports.Operation = Operation;
class Request extends DomainElement {
    constructor(raw, id, label, payloads) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.payloads = payloads;
        this.kind = "Request";
    }
}
exports.Request = Request;
class Response extends DomainElement {
    constructor(raw, id, label, status, payloads) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.status = status;
        this.payloads = payloads;
        this.kind = "Response";
    }
}
exports.Response = Response;
class Payload extends DomainElement {
    constructor(raw, id, label, mediaType, schema) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.mediaType = mediaType;
        this.schema = schema;
        this.kind = "Payload";
    }
}
exports.Payload = Payload;
class Schema extends DomainElement {
    constructor(raw, id, label, shape) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.shape = shape;
        this.kind = "Schema";
    }
}
exports.Schema = Schema;
class IncludeRelationship extends DomainElement {
    constructor(raw, id, target, label) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.target = target;
        this.label = label;
        this.kind = "Include";
    }
    ;
}
exports.IncludeRelationship = IncludeRelationship;
// Factory
class DomainModel {
    constructor(jsonld) {
        this.jsonld = jsonld;
        this.elements = {};
        this.root = this.process();
    }
    process() {
        console.log("BUILDING DOMAIN MODEL FOR " + this.jsonld["@id"]);
        const encoded = this.jsonld;
        if (has_type(encoded, exports.API_DOCUMENTATION)) {
            console.log("* Processing APIDocumentation");
            return this.buildAPIDocumentation(encoded);
        }
        else if (has_type(encoded, exports.END_POINT)) {
            console.log("* Processing EndPoint");
            return this.buildEndPoint(encoded);
        }
        else if (has_type(encoded, exports.OPERATION)) {
            console.log("* Processing Operation");
            return this.buildOperation(encoded);
        }
        else if (has_type(encoded, exports.REQUEST)) {
            console.log("* Processing Response");
            return this.buildRequest(encoded);
        }
        else if (has_type(encoded, exports.RESPONSE)) {
            console.log("* Processing Response");
            return this.buildResponse(encoded);
        }
        else if (has_type(encoded, exports.PAYLOAD)) {
            console.log("* Processing Payload");
            return this.buildPayload(encoded);
        }
        else if (has_type(encoded, exports.SCHEMA)) {
            console.log("* Processing Schema");
            return this.buildSchema(encoded);
        }
        else if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            console.log("* Processing Include");
            return this.buildInclude(encoded);
        }
        else {
            return this.buildDomainElement(encoded);
        }
    }
    buildDomainElement(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building DomainElement " + encoded["@id"]);
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const element = new DomainElement(encoded, encoded["@id"], label);
        this.elements[element.id] = element;
        return element;
    }
    buildAPIDocumentation(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building APIDocumentation " + encoded["@id"]);
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const endpoints = extract_links(encoded, exports.ENDPOINT).map(elm => this.buildEndPoint(elm));
        const element = new APIDocumentation(encoded, encoded["@id"], label, endpoints);
        this.elements[element.id] = element;
        return element;
    }
    buildEndPoint(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building EndPoint " + encoded["@id"]);
        const path = extract_value(encoded, exports.PATH);
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const operations = extract_links(encoded, exports.SUPPORTED_OPERATION).map(elm => this.buildOperation(elm));
        const element = new EndPoint(encoded, encoded["@id"], path, label, operations);
        this.elements[element.id] = element;
        return element;
    }
    buildOperation(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Operation " + encoded["@id"]);
        const method = extract_value(encoded, exports.METHOD);
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const requests = extract_links(encoded, exports.EXPECTS).map(elm => this.buildRequest(elm));
        const responses = extract_links(encoded, exports.RETURNS).map(elm => this.buildResponse(elm));
        const element = new Operation(encoded, encoded["@id"], label, method, requests, responses);
        this.elements[element.id] = element;
        return element;
    }
    buildResponse(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Response " + encoded["@id"]);
        const status = extract_value(encoded, exports.STATUS_CODE);
        const payloads = extract_links(encoded, exports.RESPONSE_PAYLOAD).map(elm => this.buildPayload(elm));
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const element = new Response(encoded, encoded["@id"], label, status, payloads);
        this.elements[element.id] = element;
        return element;
    }
    buildRequest(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        const payloads = extract_links(encoded, exports.RESPONSE_PAYLOAD).map(elm => this.buildPayload(elm));
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const element = new Request(encoded, encoded["@id"], label, payloads);
        this.elements[element.id] = element;
        return element;
    }
    buildPayload(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Payload " + encoded["@id"]);
        const mediaType = extract_value(encoded, exports.MEDIA_TYPE);
        const schema = this.buildSchema(extract_link(encoded, exports.PAYLOAD_SCHEMA));
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const element = new Payload(encoded, encoded["@id"], label, mediaType, schema);
        this.elements[element.id] = element;
        return element;
    }
    buildSchema(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, exports.INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        const shape = extract_link(encoded, exports.SCHEMA_SHAPE);
        const label = extract_value(shape, exports.NAME) || extract_value(encoded, exports.LABEL);
        const element = new Schema(encoded, encoded["@id"], label, shape);
        this.elements[element.id] = element;
        return element;
    }
    buildInclude(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        console.log("* Building Include " + encoded["@id"]);
        console.log(JSON.stringify(encoded, null, 2));
        const label = extract_value(encoded, exports.NAME) || extract_value(encoded, exports.LABEL);
        const target = extract_link(encoded, exports.TARGET);
        console.log("  Target: " + target["@id"]);
        const element = new IncludeRelationship(encoded, encoded["@id"], target["@id"], label);
        this.elements[element.id] = element;
        return element;
    }
}
exports.DomainModel = DomainModel;
//# sourceMappingURL=domain_model.js.map