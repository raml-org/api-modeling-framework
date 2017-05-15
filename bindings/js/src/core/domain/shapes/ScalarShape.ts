/**
 * Created by antoniogarrote on 10/05/2017.
 */

import {SH_NS, Shape, SHAPES_NS, XSDDataType} from "./Shape";

export class ScalarShape extends Shape {

    public getDatatype(): XSDDataType | undefined {
        return this.getRdfId(`${SH_NS}datatype`);
    }

    public setDatatype(type: XSDDataType | null) {
        this.setRdfId(`${SH_NS}datatype`, type);
    }

    public static build(id: string): ScalarShape {
        return new ScalarShape({"@id": id, "@type": [`${SH_NS}Shape`, `${SHAPES_NS}Scalar`]});
    }
}