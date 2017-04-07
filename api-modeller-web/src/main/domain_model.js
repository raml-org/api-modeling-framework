// Namespaces
export const HYDRA_NS = "http://www.w3.org/ns/hydra/core#";
export const DOCUMENT_NS = "http://raml.org/vocabularies/document#";
export const HTTP_NS = "http://raml.org/vocabularies/http#";
export const SHAPES_NS = "http://raml.org/vocabularies/shapes#";
export const RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
export const RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
export const SHACL_NS = "http://www.w3.org/ns/shacl#";
export const XSD_NS = "http://www.w3.org/2001/XMLSchema#";
export const SCHEMA_ORG_NS = "http://schema.org/";
export const NS_MAPPING = {};
NS_MAPPING[HYDRA_NS] = "hydra";
NS_MAPPING[DOCUMENT_NS] = "doc";
NS_MAPPING[HTTP_NS] = "http";
NS_MAPPING[SHAPES_NS] = "shapes";
NS_MAPPING[SHACL_NS] = "shacl";
NS_MAPPING[RDF_NS] = "rdf";
NS_MAPPING[RDFS_NS] = "rdfs";
NS_MAPPING[XSD_NS] = "xsd";
NS_MAPPING[SCHEMA_ORG_NS] = "schema-org";
// RDF Classes
export const DOMAIN_ELEMENT = DOCUMENT_NS + "DomainElement";
export const API_DOCUMENTATION = HTTP_NS + "APIDocumentation";
export const END_POINT = HTTP_NS + "EndPoint";
export const OPERATION = HYDRA_NS + "Operation";
export const RESPONSE = HTTP_NS + "Response";
export const REQUEST = HTTP_NS + "Request";
export const PAYLOAD = HTTP_NS + "Payload";
export const SCHEMA = HTTP_NS + "Schema";
export const INCLUDE_RELATIONSHIP = DOCUMENT_NS + "IncludeRelationship";
// RDF Properties
export const LABEL = DOCUMENT_NS + "label";
export const NAME = SCHEMA_ORG_NS + "name";
export const ENCODES = DOCUMENT_NS + "encodes";
export const ENDPOINT = HTTP_NS + "endpoint";
export const PATH = HTTP_NS + "path";
export const SUPPORTED_OPERATION = HYDRA_NS + "supportedOperation";
export const METHOD = HYDRA_NS + "method";
export const RETURNS = HYDRA_NS + "returns";
export const EXPECTS = HYDRA_NS + "expects";
export const STATUS_CODE = HYDRA_NS + "statusCode";
export const RESPONSE_PAYLOAD = HTTP_NS + "payload";
export const MEDIA_TYPE = HTTP_NS + "mediaType";
export const SCHEMA_SHAPE = HTTP_NS + "shape";
export const PAYLOAD_SCHEMA = HTTP_NS + "schema";
export const TARGET = DOCUMENT_NS + "target";
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
export class DomainElement {
    constructor(raw, id, label) {
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.kind = "DomainElement";
    }
}
export class APIDocumentation extends DomainElement {
    constructor(raw, id, label, endpoints) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.endpoints = endpoints;
        this.kind = "APIDocumentation";
    }
}
export class EndPoint extends DomainElement {
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
export class Operation extends DomainElement {
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
export class Request extends DomainElement {
    constructor(raw, id, label, payloads) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.payloads = payloads;
        this.kind = "Request";
    }
}
export class Response extends DomainElement {
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
export class Payload extends DomainElement {
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
export class Schema extends DomainElement {
    constructor(raw, id, label, shape) {
        super(raw, id, label);
        this.raw = raw;
        this.id = id;
        this.label = label;
        this.shape = shape;
        this.kind = "Schema";
    }
}
export class IncludeRelationship extends DomainElement {
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
// Factory
export class DomainModel {
    constructor(jsonld) {
        this.jsonld = jsonld;
        this.elements = {};
        this.root = this.process();
    }
    process() {
        console.log("BUILDING DOMAIN MODEL FOR " + this.jsonld["@id"]);
        const encoded = this.jsonld;
        if (has_type(encoded, API_DOCUMENTATION)) {
            console.log("* Processing APIDocumentation");
            return this.buildAPIDocumentation(encoded);
        }
        else if (has_type(encoded, END_POINT)) {
            console.log("* Processing EndPoint");
            return this.buildEndPoint(encoded);
        }
        else if (has_type(encoded, OPERATION)) {
            console.log("* Processing Operation");
            return this.buildOperation(encoded);
        }
        else if (has_type(encoded, REQUEST)) {
            console.log("* Processing Response");
            return this.buildRequest(encoded);
        }
        else if (has_type(encoded, RESPONSE)) {
            console.log("* Processing Response");
            return this.buildResponse(encoded);
        }
        else if (has_type(encoded, PAYLOAD)) {
            console.log("* Processing Payload");
            return this.buildPayload(encoded);
        }
        else if (has_type(encoded, SCHEMA)) {
            console.log("* Processing Schema");
            return this.buildSchema(encoded);
        }
        else if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
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
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building DomainElement " + encoded["@id"]);
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const element = new DomainElement(encoded, encoded["@id"], label);
        this.elements[element.id] = element;
        return element;
    }
    buildAPIDocumentation(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building APIDocumentation " + encoded["@id"]);
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const endpoints = extract_links(encoded, ENDPOINT).map(elm => this.buildEndPoint(elm));
        const element = new APIDocumentation(encoded, encoded["@id"], label, endpoints);
        this.elements[element.id] = element;
        return element;
    }
    buildEndPoint(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building EndPoint " + encoded["@id"]);
        const path = extract_value(encoded, PATH);
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const operations = extract_links(encoded, SUPPORTED_OPERATION).map(elm => this.buildOperation(elm));
        const element = new EndPoint(encoded, encoded["@id"], path, label, operations);
        this.elements[element.id] = element;
        return element;
    }
    buildOperation(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Operation " + encoded["@id"]);
        const method = extract_value(encoded, METHOD);
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const requests = extract_links(encoded, EXPECTS).map(elm => this.buildRequest(elm));
        const responses = extract_links(encoded, RETURNS).map(elm => this.buildResponse(elm));
        const element = new Operation(encoded, encoded["@id"], label, method, requests, responses);
        this.elements[element.id] = element;
        return element;
    }
    buildResponse(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Response " + encoded["@id"]);
        const status = extract_value(encoded, STATUS_CODE);
        const payloads = extract_links(encoded, RESPONSE_PAYLOAD).map(elm => this.buildPayload(elm));
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const element = new Response(encoded, encoded["@id"], label, status, payloads);
        this.elements[element.id] = element;
        return element;
    }
    buildRequest(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        const payloads = extract_links(encoded, RESPONSE_PAYLOAD).map(elm => this.buildPayload(elm));
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const element = new Request(encoded, encoded["@id"], label, payloads);
        this.elements[element.id] = element;
        return element;
    }
    buildPayload(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        console.log("* Building Payload " + encoded["@id"]);
        const mediaType = extract_value(encoded, MEDIA_TYPE);
        const schema = this.buildSchema(extract_link(encoded, PAYLOAD_SCHEMA));
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const element = new Payload(encoded, encoded["@id"], label, mediaType, schema);
        this.elements[element.id] = element;
        return element;
    }
    buildSchema(encoded) {
        if (encoded == null || encoded["@id"] == null) {
            return undefined;
        }
        if (has_type(encoded, INCLUDE_RELATIONSHIP)) {
            return this.buildInclude(encoded);
        }
        const shape = extract_link(encoded, SCHEMA_SHAPE);
        const label = extract_value(shape, NAME) || extract_value(encoded, LABEL);
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
        const label = extract_value(encoded, NAME) || extract_value(encoded, LABEL);
        const target = extract_link(encoded, TARGET);
        console.log("  Target: " + target["@id"]);
        const element = new IncludeRelationship(encoded, encoded["@id"], target["@id"], label);
        this.elements[element.id] = element;
        return element;
    }
}
//# sourceMappingURL=domain_model.js.map