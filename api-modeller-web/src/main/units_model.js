"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const utils_1 = require("../utils");
const domain_model_1 = require("./domain_model");
class DomainElement {
    constructor(id, raw, label, elementClass, isAbstract) {
        this.id = id;
        this.label = label;
        this.elementClass = elementClass;
        this.kind = "DomainElement";
        this.domain = new domain_model_1.DomainModel(raw);
    }
}
exports.DomainElement = DomainElement;
class DocumentDeclaration {
    constructor(id, raw, label, elementClass, traitAlias) {
        this.id = id;
        this.label = label;
        this.elementClass = elementClass;
        this.traitAlias = traitAlias;
        this.kind = "DocumentDeclaration";
        this.domain = new domain_model_1.DomainModel(raw);
    }
}
exports.DocumentDeclaration = DocumentDeclaration;
class Document {
    constructor(id, label, references, declares, encodes) {
        this.id = id;
        this.label = label;
        this.references = references;
        this.declares = declares;
        this.encodes = encodes;
        this.kind = "Document";
    }
}
exports.Document = Document;
class Fragment {
    constructor(id, label, references, encodes) {
        this.id = id;
        this.label = label;
        this.references = references;
        this.encodes = encodes;
        this.kind = "Fragment";
    }
}
exports.Fragment = Fragment;
class Module {
    constructor(id, label, references, declares) {
        this.id = id;
        this.label = label;
        this.references = references;
        this.declares = declares;
        this.kind = "Module";
    }
}
exports.Module = Module;
function consumePromises(ps) {
    return __awaiter(this, void 0, void 0, function* () {
        if (ps.length > 0) {
            const next = ps.pop();
            if (next != null) {
                yield next;
            }
            yield consumePromises(ps);
        }
    });
}
class UnitModel {
    constructor(model) {
        this.model = model;
    }
    process(modelLevel, cb) {
        const acc = { "documents": [], "fragments": [], "modules": [] };
        const references = this.model.references();
        const promises = references.map(reference => {
            if (modelLevel == "document" || reference == this.model.location()) {
                return new Promise((resolve, reject) => {
                    let nestedModel = this.model.nestedModel(reference);
                    nestedModel.toAPIModelProcessed(modelLevel, false, false, { "source-maps?": true }, (err, jsonld) => {
                        if (err != null) {
                            reject(err);
                        }
                        else {
                            if (this.isDocument(jsonld)) {
                                this.parseDocument(jsonld, acc);
                            }
                            else if (this.isFragment(jsonld)) {
                                this.parseFragment(jsonld, acc);
                            }
                            else if (this.isModule(jsonld)) {
                                this.parseModule(jsonld, acc);
                            }
                            resolve(undefined);
                        }
                    });
                });
            }
            else {
                return null;
            }
        });
        consumePromises(promises)
            .then((_) => {
            this.foldReferences(acc);
            cb(null, acc);
        }).catch((e) => cb(e, null));
    }
    isDocument(doc) {
        return (doc["@type"] || []).filter((type) => {
            return type === "http://raml.org/vocabularies/document#Document";
        }).length > 0;
    }
    isModule(doc) {
        return (doc["@type"] || []).filter((type) => {
            return type === "http://raml.org/vocabularies/document#Module";
        }).length > 0;
    }
    isFragment(doc) {
        return (doc["@type"] || []).filter((type) => {
            return type === "http://raml.org/vocabularies/document#Fragment";
        }).length > 0;
    }
    parseDocument(doc, acc) {
        const docId = doc["@id"];
        const declares = (doc["http://raml.org/vocabularies/document#declares"] || []).map((declaration) => {
            return new DocumentDeclaration(declaration["@id"], declaration, utils_1.nestedLabel(docId, declaration["@id"]), (declaration["@type"] || [])[0], this.extractTag("is-trait-tag", declaration));
        });
        const references = this.extractReferences(doc);
        acc.documents.push(new Document(docId, utils_1.label(docId), references, declares, this.extractEncodedElement(doc)));
    }
    parseFragment(doc, acc) {
        const references = this.extractReferences(doc);
        acc.fragments.push(new Fragment(doc["@id"], utils_1.label(doc["@id"]), references, this.extractEncodedElement(doc)));
    }
    parseModule(doc, acc) {
        const docId = doc["@id"];
        const declares = (doc["http://raml.org/vocabularies/document#declares"] || []).map((declaration) => {
            return new DocumentDeclaration(declaration["@id"], declaration, utils_1.nestedLabel(docId, declaration["@id"]), (declaration["@type"] || [])[0], this.extractTag("is-trait-tag", declaration));
        });
        const references = this.extractReferences(doc);
        acc.modules.push(new Module(docId, utils_1.label(docId), references, declares));
    }
    flatten(array, mutable) {
        const toString = Object.prototype.toString;
        const arrayTypeStr = '[object Array]';
        const result = [];
        const nodes = (mutable && array) || array.slice();
        let node;
        if (!array.length) {
            return result;
        }
        node = nodes.pop();
        do {
            if (toString.call(node) === arrayTypeStr) {
                nodes.push.apply(nodes, node);
            }
            else {
                result.push(node);
            }
        } while (nodes.length && (node = nodes.pop()) !== undefined);
        result.reverse(); // we reverse result to restore the original order
        return result;
    }
    extractTag(tagId, node) {
        try {
            const sources = (node["http://raml.org/vocabularies/document#source"] || []);
            const tagFound = this
                .flatten(sources.map(source => source["http://raml.org/vocabularies/document#tag"] || []), true)
                .filter(tag => {
                return tag["http://raml.org/vocabularies/document#tagId"][0]["@value"] === tagId;
            })[0];
            if (tagFound) {
                return tagFound["http://raml.org/vocabularies/document#tagValue"][0]["@value"];
            }
            else {
                return undefined;
            }
        }
        catch (e) {
            return undefined;
        }
    }
    extractEncodedElement(node) {
        const encoded = (node["http://raml.org/vocabularies/document#encodes"] || [])[0];
        if (encoded != null) {
            const isAbstract = encoded["@type"].find(t => t === "http://raml.org/vocabularies/document#AbstractDomainElement");
            return new DomainElement(encoded["@id"], encoded, utils_1.nestedLabel(node["@id"], encoded["@id"]), encoded["@type"][0], isAbstract);
        }
        else {
            return undefined;
        }
    }
    extractReferences(doc) {
        const references = (doc["http://raml.org/vocabularies/document#references"] || []);
        return references.map((ref) => ref["@id"]);
    }
    foldReferences(acc) {
        const mapping = {};
        acc.documents.forEach(doc => mapping[doc.id] = doc);
        acc.fragments.forEach(doc => mapping[doc.id] = doc);
        acc.modules.forEach(doc => mapping[doc.id] = doc);
        acc.documents.forEach((doc) => {
            const references = doc.references.map((ref) => mapping[ref]);
            doc.references = references;
        });
        acc.fragments.forEach((doc) => {
            const references = doc.references.map((ref) => mapping[ref]);
            doc.references = references;
        });
        acc.modules.forEach((doc) => {
            const references = doc.references.map((ref) => mapping[ref]);
            doc.references = references;
        });
    }
}
exports.UnitModel = UnitModel;
//# sourceMappingURL=units_model.js.map