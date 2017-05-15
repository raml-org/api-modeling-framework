/**
 * Created by antoniogarrote on 10/05/2017.
 */

import {SH_NS, Shape, SHAPES_NS} from "./Shape";

export class ArrayShape extends Shape {

    public getItems(): Shape[] {
        return this.getRdfObjects(`${SHAPES_NS}items`).map(jsonld => Shape.fromRawModel(jsonld));
    }

    public setItems(items: Shape[]) {
        this.setRdfObject(`${SHAPES_NS}items`, items);
    }

    public static build(id: string): ArrayShape {
        return new ArrayShape({"@id": id, "@type": [`${SH_NS}Shape`, `${SHAPES_NS}Array`]})
    }
}