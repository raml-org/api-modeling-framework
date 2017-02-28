import {ModelProxy, ModelLevel} from "./model_proxy";

// Namespaces
export const HYDRA_NS: string = "http://www.w3.org/ns/hydra/core#";
export const DOCUMENT_NS: string = "http://raml.org/vocabularies/document#";
export const HTTP_NS: string = "http://raml.org/vocabularies/http#";

// RDF Classes
export const DOMAIN_ELEMENT: string = DOCUMENT_NS + "DomainElement";
export const API_DOCUMENTATION: string = HTTP_NS + "APIDocumentation";
export const END_POINT: string = HTTP_NS + "EndPoint";
export const OPERATION: string = HYDRA_NS + "Operation";
export const RESPONSE: string = HTTP_NS + "Response";
export const PAYLOAD: string = HTTP_NS + "Payload";

// RDF Properties
export const LABEL: string = DOCUMENT_NS + "label";
export const ENCODES: string = DOCUMENT_NS + "encodes";
export const ENDPOINT: string = HTTP_NS + "endpoint";
export const PATH: string = HTTP_NS + "path";
export const SUPPORTED_OPERATION: string = HYDRA_NS + "supportedOperation";
export const METHOD: string = HYDRA_NS + "method";
export const RETURNS: string = HYDRA_NS + "returns";
export const STATUS_CODE: string = HYDRA_NS + "statusCode";
export const RESPONSE_PAYLOAD: string = HTTP_NS + "payload";
export const PAYLOAD_SHAPE: string = HTTP_NS + "shape";

// Utility functions
function extract_link(node: any, property: string) {
    return ((node[property] || [])[0]) || {};
}

function extract_links(node: any, property: string) {
    return (node[property] || []);
}

function extract_value(node: any, property: string) {
    const value = ((node[property] || [])[0]) || {};
    return value["@value"];
}

function has_type(node, type) {
    const types = node["@type"] || [];
    console.log("CHECKING TYPES");
    console.log(JSON.stringify(types));
    return types.find(t => t === type) != null;
}

// Model Classes
export type DomainElementKind = "APIDocumentation" | "EndPoint" | "Operation" | "Response" | "Payload" | "DomainElement";

export interface DomainModelElement {
    id: string;
    raw: any;
    label: string;
    kind: DomainElementKind;
}

export class DomainElement implements DomainModelElement {
    kind: DomainElementKind = "DomainElement";
    constructor (public raw: any, public id: string, public label: string) {}
}

export class APIDocumentation extends DomainElement {
    kind: DomainElementKind = "APIDocumentation";

    constructor(public raw: any, public id: string, public label: string, public endpoints: EndPoint[]) { super(raw, id, label) }
}

export class EndPoint extends DomainElement {
    kind: DomainElementKind = "EndPoint";

    constructor(public raw: any, public id: string, public label: string, public path: string, public operations: Operation[]) { super(raw, id, label) }
}

export class Operation extends DomainElement {
    kind: DomainElementKind = "Operation";

    constructor(public raw: any, public id: string, public label: string, public method: string, public responses: Response[]) { super(raw, id, label) }
}

export class Response extends DomainElement {
    kind: DomainElementKind = "Response";

    constructor(public raw: any, public id: string, public label: string, public status: string, public payload: Payload | undefined) { super(raw, id, label) }
}

export class Payload extends DomainElement {
    kind: DomainElementKind = "Payload";

    constructor(public raw: any, public id: string, public label: string, public shape: any | undefined) { super(raw, id, label) }
}

// Factory
export class DomainModel {

    public elements: {[id: string] : DomainElement} = {};
    public root: DomainElement | undefined;

    constructor(public jsonld: any) {
        this.root = this.process();
    }

    public process() {
        console.log("BUILDING DOMAIN MODEL FOR " + this.jsonld["@id"]);
        const encoded = this.jsonld;
        if (has_type(encoded, API_DOCUMENTATION)) {
            console.log("* Processing APIDocumentation");
            return this.buildAPIDocumentation(encoded);
        } else if(has_type(encoded, END_POINT)) {
            console.log("* Processing EndPoint");
            return this.buildEndPoint(encoded);
        } else if(has_type(encoded, OPERATION)) {
            console.log("* Processing Operation");
            return this.buildOperation(encoded);
        } else if(has_type(encoded, RESPONSE)) {
            console.log("* Processing Response");
            return this.buildResponse(encoded);
        } else if(has_type(encoded, PAYLOAD)) {
            console.log("* Processing Payload");
            return this.buildPayload(encoded);
        } else {
            return this.buildDomainElement(encoded);
        }
    }


    private buildDomainElement(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building DomainElement " + encoded["@id"]);
        const label = extract_value(encoded, LABEL);
        const element = new DomainElement(encoded, encoded["@id"], label);
        this.elements[element.id] = element;
        return element;
    }

    private buildAPIDocumentation(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building APIDocumentation " + encoded["@id"]);
        const label = extract_value(encoded, LABEL);
        const endpoints = extract_links(encoded, ENDPOINT).map(elm => this.buildEndPoint(elm));
        const element = new APIDocumentation(encoded, encoded["@id"], label, endpoints);
        this.elements[element.id] = element;
        return element;
    }

    private buildEndPoint(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building EndPoint " + encoded["@id"]);
        const path = extract_value(encoded, PATH);
        const label = extract_value(encoded, LABEL);
        const operations = extract_links(encoded, SUPPORTED_OPERATION).map(elm => this.buildOperation(elm));
        const element = new EndPoint(encoded, encoded["@id"], path, label, operations);
        this.elements[element.id] = element;
        return element;
    }

    private buildOperation(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building Operation " + encoded["@id"]);
        const method = extract_value(encoded, METHOD);
        const label = extract_value(encoded, LABEL);
        const responses = extract_links(encoded, RETURNS).map(elm => this.buildResponse(elm));
        const element = new Operation(encoded, encoded["@id"], label, method, responses);
        this.elements[element.id] = element;
        return element;
    }


    private buildResponse(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building Response " + encoded["@id"]);
        const status = extract_value(encoded, STATUS_CODE);
        const payload = this.buildPayload(extract_link(encoded, RESPONSE_PAYLOAD));
        const label = extract_value(encoded, LABEL);
        const element = new Response(encoded, encoded["@id"], label, status, payload);
        this.elements[element.id] = element;
        return element;
    }

    private buildPayload(encoded: any) {
        if (encoded == null) { return undefined }
        console.log("* Building Payload " + encoded["@id"]);
        const shape = extract_link(encoded, PAYLOAD_SHAPE);
        const label = extract_value(encoded, LABEL);
        const element = new Payload(encoded, encoded["@id"], label, shape);
        this.elements[element.id] = element;
        return element;
    }
}