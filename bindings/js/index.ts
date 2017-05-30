/**
 * Created by antoniogarrote on 05/05/2017.
 */
import { RAMLParser } from "./src/parsers/RAMLParser";
import { OpenAPIParser } from "./src/parsers/OpenAPIParser";
import { AMFJSONLDParser } from "./src/parsers/AMFJSONLDParser";
import { RAMLGenerator } from "./src/generators/RAMLGenerator";
import { OpenAPIGenerator } from "./src/generators/OpenAPIGenerator";
import { AMFJSONDGenerator } from "./src/generators/AMFJSONLDGenerator";
import { Model } from "./src/core/Model";
import { DeclaresDomainModel } from "./src/core/document/DeclaresDomainModel";
import { DocumentModel } from "./src/core/document/DocumentModel";
import { EncodesDomainModel } from "./src/core/document/EncodesDomainModel";
import { Fragment } from "./src/core/document/Fragment";
import { Module } from "./src/core/document/Module";
import { Document } from "./src/core/document/Document";
import { ArrayShape } from "./src/core/domain/shapes/ArrayShape";
import { FileUploadShape } from "./src/core/domain/shapes/FileUploadShape";
import { JSONSchemaShape } from "./src/core/domain/shapes/JSONSchemaShape";
import { NodeShape } from "./src/core/domain/shapes/NodeShape";
import { PropertyShape } from "./src/core/domain/shapes/PropertyShape";
import { ScalarShape } from "./src/core/domain/shapes/ScalarShape";
import { Shape } from "./src/core/domain/shapes/Shape";
import { XMLSchemaShape } from "./src/core/domain/shapes/XMLSchemaShape";
import { APIDocumentation } from "./src/core/domain/APIDocumentation";
import { EndPoint } from "./src/core/domain/EndPoint";
import { GenericOperationUnit } from "./src/core/domain/GenericOperationUnit";
import { GenericParameter } from "./src/core/domain/GenericParameter";
import { GenericTag } from "./src/core/domain/GenericTag";
import { Header } from "./src/core/domain/Header";
import { Operation } from "./src/core/domain/Operation";
import { Parameter } from "./src/core/domain/Parameter";
import { Payload } from "./src/core/domain/Payload";
import { SourceMap } from "./src/core/domain/SourceMap";
import { Type } from "./src/core/domain/Type";
import { Request } from "./src/core/domain/Request";
import { Response } from "./src/core/domain/Response";
export type URL = string;

declare var require: any

export const core = {
    Model: Model,
    document: {
        Document: Document,
        DocumentModel: DocumentModel,
        Fragment: Fragment,
        Module: Module
    },
    domain: {
        shapes: {
            ArrayShape: ArrayShape,
            FileUploadShape: FileUploadShape,
            JSONSchemaShape: JSONSchemaShape,
            NodeShape: NodeShape,
            PropertyShape: PropertyShape,
            ScalarShape: ScalarShape,
            Shape: Shape,
            XMLSchemaShape: XMLSchemaShape
        },
        APIDocumentation: APIDocumentation,
        EndPoint: EndPoint,
        GenericOperationUnit: GenericOperationUnit,
        GenericParametet: GenericParameter,
        GenericTag: GenericTag,
        Header: Header,
        Operation: Operation,
        Parameter: Parameter,
        Payload: Payload,
        Request: Request,
        Response: Response,
        SourceMap: SourceMap,
        Type: Type
    }
};
export const Clojure = require("./src/Clojure");

/**
 * Facade class providing access to the main IO facilities in the library
 */
export class AMF {
    /**
     * Builds a RAML to AMF parser
     * @return
     */
    public static RAMLParser = new RAMLParser();

    /**
     * Builds an OpenAPI to AMF parser
     * @return
     */
    public static OpenAPIParser = new OpenAPIParser();

    /**
     * Builds a AMF encoded JSON-LD to AMF parser
     * @return
     */
    public static JSONLDParser = new AMFJSONLDParser();

    /**
     * Builds a AMF to RAML generator
     * @return
     */
    public static RAMLGenerator = new RAMLGenerator();

    /**
     * Builds a AMF to OpenAPI generator
     * @return
     */
    public static OpenAPIGenerator = new OpenAPIGenerator();

    /**
     * Builds a AMF to JSON-LD generator
     * @return
     */
    public static JSONLDGenerator = new AMFJSONDGenerator();
}
