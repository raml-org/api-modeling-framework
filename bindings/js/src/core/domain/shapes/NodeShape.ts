/**
 * Created by antoniogarrote on 10/05/2017.
 */

import {SH_NS, Shape} from "./Shape";
import {PropertyShape} from "./PropertyShape";

export class NodeShape extends Shape {

    public getPropertyShapes(): PropertyShape[] {
        let shapes = this.rawModel[`${SH_NS}property`] || [];
        return shapes.map(jsonld => new PropertyShape(jsonld));
    }

    public getClosed(): boolean {
        return this.getValue<boolean>(`${SH_NS}closed`) || false;
    }

    public setClosed(closed: boolean) {
        this.setValue(`${SH_NS}closed`, closed);
    }

    public static build(id: string): NodeShape {
        return new NodeShape({"@id": id, "@type": [`${SH_NS}Shape`, `${SH_NS}NodeShape`]})
    }
}