/**
 * Created by antoniogarrote on 10/05/2017.
 */

import {DomainModel} from "../DomainModel";

export const SH_NS = "http://www.w3.org/ns/shacl#";
export const SHAPES_NS = "http://raml.org/vocabularies/shapes#";
export type XSDDataType = string;

export class Shape extends DomainModel {

    public getId(): string {
        return this.rawModel["@id"];
    }

    public getInherits(): Shape[] {
        return this.getRdfObjects(`${SHAPES_NS}inherits`).map(jsonld => Shape.fromRawModel(jsonld));
    }

    public setInherits(shapes: Shape[]) {
        this.setRdfObject(`${SHAPES_NS}inherits`, shapes);
    }

    public static build(id: string): Shape {
        return new Shape({"@id": id, "@type": [`${SH_NS}Shape`]})
    }

    public static fromRawModel(rawModel: any): Shape {
        if (this.findShapeClass(rawModel, `${SH_NS}NodeShape`)) {
            return new NodeShape(rawModel);
        } else if (this.findShapeClass(rawModel, `${SH_NS}PropertyShape`)) {
            return new PropertyShape(rawModel);
        } else if (this.findShapeClass(rawModel, `${SHAPES_NS}Scalar`)){
            return new ScalarShape(rawModel)
        } else if (this.findShapeClass(rawModel, `${SHAPES_NS}Array`)) {
            return new ArrayShape(rawModel);
        } else if (this.findShapeClass(rawModel, `${SHAPES_NS}FileUpload`)) {
            return new  FileUploadShape(rawModel);
        } else if (this.findShapeClass(rawModel, `${SHAPES_NS}XMLSchema`)) {
            return new XMLSchemaShape(rawModel);
        } else if (this.findShapeClass(rawModel, `${SHAPES_NS}JSONSchema`)) {
            return new JSONSchemaShape(rawModel);
        } else {
            return new Shape(rawModel);
        }
    }

    protected static findShapeClass(shape: any, shapeClass: string): boolean {
        let types = shape["@type"];
        types = this.ensureArray(types);

        return types.find(t => t === shapeClass);
    }

    protected getValue<T>(p): T | undefined {
        let res = Shape.ensureArray(this.rawModel[p]);
        if (res.length > 0) {
            return res[0]["@value"] as T | undefined;
        } else {
            return undefined;
        }
    }

    protected getValues<T>(p): T[] | undefined {
        let res = Shape.ensureArray(this.rawModel[p]);
        if (res.length > 0) {
            return res.map(jsonld => jsonld["@value"]) as T[] | undefined;
        } else {
            return undefined;
        }
    }

    protected setValue(p: string, x: any) {
        if (x == null) {
            delete this.rawModel[p];
        } else {
            this.rawModel[p] = [{"@value": x}];
        }
    }

    protected setValues(p: string, x: any) {
        if (x == null) {
            delete this.rawModel[p];
        } else {
            this.rawModel[p] = Shape.ensureArray(x).map(x => [{"@value": x}]);
        }
    }

    protected setRdfObject(p: string, object: any | null) {
        if (object == null) {
            delete this.rawModel[p];
        } else {
            this.rawModel[p] = Shape.ensureArray(object);
        }
    }

    protected getRdfObject(p): any | undefined {
        let res = Shape.ensureArray(this.rawModel[p]);
        if (res.length > 0) {
            return res[0]
        }
    }

    protected getRdfObjects(p): any[] | undefined {
        return Shape.ensureArray(this.rawModel[p]);
    }

    protected getRdfId(p): string | undefined {
        let res = Shape.ensureArray(this.rawModel[p]);
        if (res.length > 0) {
            return res[0]["@id"]
        }
    }

    protected setRdfId(p: string, id: string | null) {
        if (id == null) {
            delete this.rawModel[p];
        } else {
            this.rawModel[p] = [{"@id": id}];
        }
    }

    protected static ensureArray(x: any): any[] {
        if (x == null) {
            return []
        } else if(x.constructor !== Array) {
            return [x];
        } else {
            return x;
        }
    }
}

import {NodeShape} from "./NodeShape";
import {ScalarShape} from "./ScalarShape";
import {PropertyShape} from "./PropertyShape";
import {ArrayShape} from "./ArrayShape";
import {FileUploadShape} from "./FileUploadShape";
import {XMLSchemaShape} from "./XMLSchemaShape";
import {JSONSchemaShape} from "./JSONSchemaShape";