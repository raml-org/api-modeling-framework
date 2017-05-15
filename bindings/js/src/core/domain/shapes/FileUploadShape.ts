/**
 * Created by antoniogarrote on 10/05/2017.
 */

import {SH_NS, Shape, SHAPES_NS} from "./Shape";

export class FileUploadShape extends Shape {

    public getFileType(): string[] | undefined {
        return this.getValues<string>(`${SHAPES_NS}fileType`);
    }

    public setSchemaRaw(fileTypes: string[] | null) {
        this.setValues(`${SHAPES_NS}fileType`, fileTypes);
    }

    public static build(id: string): FileUploadShape {
        return new FileUploadShape({"@id": id, "@type": [`${SH_NS}Shape`, `${SHAPES_NS}FileUpload`]})
    }
}