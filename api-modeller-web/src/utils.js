"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function label(uri) {
    if (uri.indexOf("#") > -1) {
        const hashPart = uri.split("#")[1];
        const base = uri.split("#")[0];
        return "/" + base.split("/").pop() + "#" + hashPart;
    }
    else {
        return ("/" + uri.split("/").pop()) || uri;
    }
}
exports.label = label;
function nestedLabel(parent, uri) {
    if (uri.indexOf(parent) > -1) {
        return uri.replace(parent, "");
    }
    else {
        return label(uri);
    }
}
exports.nestedLabel = nestedLabel;
//# sourceMappingURL=utils.js.map