import {extract_value, LABEL, NAME} from "./main/domain_model";
export function label(uri: string): string {
    if (uri.indexOf("#") > -1) {
        const hashPart = uri.split("#")[1];
        const base = uri.split("#")[0];
        return "/" + base.split("/").pop() + "#" + hashPart;
    } else {
        return ("/" + uri.split("/").pop()) || uri;
    }
}

export function nestedLabel(parent: any, child: any): string {
    const uri = child["@id"];
    const label = extract_value(child, NAME) || extract_value(child, LABEL);
    if (label != null) {
        return label;
    } else {
        if (uri.indexOf(parent["@id"]) > -1) {
            return uri.replace(parent["@id"], "");
        } else {
            this.label(uri);
        }
    }
}