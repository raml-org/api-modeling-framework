/**
 * Created by antoniogarrote on 10/05/2017.
 */
import {SH_NS, Shape, SHAPES_NS, XSDDataType} from "./Shape";

export class PropertyShape extends Shape {

    public getPath(): string | undefined {
        return this.getRdfId(`${SH_NS}path`);
    }

    public setPath(path: string | null) {
        this.setRdfId(`${SH_NS}path`, path);
    }

    public getPropertyLabel(): string | undefined {
        return this.getValue<string>(`${SHAPES_NS}propertyLabel`);
    }

    public setPropertyLabel(label: string | null) {
        this.setValue(`${SHAPES_NS}propertyLabel`, label);
    }

    public getMinCount(): number | undefined {
        return this.getValue<number>(`${SH_NS}minCount`);
    }

    public setMinCount(count: number | null) {
        this.setValue(`${SH_NS}minCount`, count);
    }

    public getMaxCount(): number | undefined {
        return this.getValue<number>(`${SH_NS}maxCount`);
    }

    public setMaxCount(count: number | null) {
        this.setValue(`${SH_NS}maxCount`, count);
    }


    public getDatatype(): XSDDataType | undefined {
        return this.getRdfId(`${SH_NS}datatype`);
    }

    public setDatatype(type: XSDDataType | null) {
        this.setRdfId(`${SH_NS}datatype`, type);
    }

    public getNode(): Shape | undefined {
        const object = this.getRdfObject(`${SH_NS}node`);
        if (object != null)
            return Shape.fromRawModel(object);
    }

    public setNode(object: Shape | null) {
        if (object != null) {
            this.setRdfObject(`${SH_NS}node`, object.clojureModel());
        } else {
            this.setRdfObject(`${SH_NS}node`, null);
        }
    }

    public getOrdered(): boolean {
        return this.getValue<boolean>(`${SHAPES_NS}ordered`) || false;
    }

    public setOrdered(ordered: boolean | null) {
        this.setValue(`${SH_NS}ordered`, ordered);
    }

    public static build(id: string): PropertyShape {
        return new PropertyShape({"@id": id, "@type": [`${SH_NS}Shape`, `${SH_NS}PropertyShape`]})
    }
}