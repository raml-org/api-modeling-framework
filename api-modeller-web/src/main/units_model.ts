import { ModelProxy, ModelLevel } from "./model_proxy";
import { label, nestedLabel } from "../utils";
import { DomainModel } from "./domain_model";

export type DocumentKind = "DomainElement" | "DocumentDeclaration" | "Document" | "Fragment" | "Module";
export interface DocumentId {
    id: string;
    label: string;
    // This is required just because of Electron remoting, I cannot use instanceof and sometimes I want to know the "class"
    kind: DocumentKind;
}

export interface Unit {
    references: (string | DocumentId)[];
}

export class DomainElement implements DocumentId {
    public kind: DocumentKind = "DomainElement";
    public domain: DomainModel;
    constructor(public id: string, raw: any, public label: string, public elementClass: string, isAbstract: boolean) {
        this.domain = new DomainModel(raw);
    }
}

export class DocumentDeclaration implements DocumentId {
    public kind: DocumentKind = "DocumentDeclaration";
    public domain: DomainModel;
    constructor(public id: string, raw: any, public label: string, public elementClass: string, public traitAlias?: string) {
        this.domain = new DomainModel(raw);
    }
}

export class Document implements DocumentId, Unit {
    public kind: DocumentKind = "Document";
    constructor(public id: string, public label: string, public references: (string | DocumentId)[], public declares: DocumentDeclaration[], public encodes?: DomainElement) { }
}

export class Fragment implements DocumentId, Unit {
    public kind: DocumentKind = "Fragment";
    constructor(public id: string, public label: string, public references: (string | DocumentId)[], public encodes?: DomainElement) { }
}

export class Module implements DocumentId, Unit {
    public kind: DocumentKind = "Module";
    constructor(public id: string, public label: string, public references: (string | DocumentId)[], public declares: DocumentDeclaration[]) { }
}

function consumePromises<T>(promises: ((ke: (e) => void, kd:(T) => void) => void)[], k:(e:any | undefined) => void) {
    if (promises.length > 0) {
        const next = promises.pop();
        if (next != null) {
            next((_) => {
                consumePromises(promises, k);
            },(e) => {
                k(e);
            });
        }
    } else {
        k(undefined);
    }
}

export class UnitModel {
    constructor(public model: ModelProxy) { }

    public process(modelLevel: ModelLevel, cb) {
        const acc = { "documents": [], "fragments": [], "modules": [] };
        const references = this.model.references();
        const promises = references.map(reference => {
            if (modelLevel == "document" || reference == this.model.location()) {
                return (resolve, reject) => {
                    let nestedModel = this.model.nestedModel(reference);
                    nestedModel.toAPIModelProcessed(modelLevel, false, false, { "source-maps?": true }, (err, jsonld: any) => {
                        if (err != null) {
                            reject(err);
                        } else {
                            if (this.isDocument(jsonld)) {
                                this.parseDocument(jsonld, acc);
                            } else if (this.isFragment(jsonld)) {
                                this.parseFragment(jsonld, acc);
                            } else if (this.isModule(jsonld)) {
                                this.parseModule(jsonld, acc);
                            }
                            resolve(undefined);
                        }
                    });
                };
            } else {
                return null;
            }
        });

        consumePromises(promises, (e) => {
            if (e != null) {
                cb(e, null);
            } else {
                this.foldReferences(acc);
                cb(null, acc);
            }
        });
    }

    isDocument(doc: any) {
        return (doc["@type"] || []).filter((type: string) => {
            return type === "http://raml.org/vocabularies/document#Document"
        }).length > 0
    }

    isModule(doc: any) {
        return (doc["@type"] || []).filter((type: string) => {
            return type === "http://raml.org/vocabularies/document#Module"
        }).length > 0
    }

    isFragment(doc: any) {
        return (doc["@type"] || []).filter((type: string) => {
            return type === "http://raml.org/vocabularies/document#Fragment"
        }).length > 0
    }

    parseDocument(doc: any, acc) {
        const docId = doc["@id"];
        const declares = (doc["http://raml.org/vocabularies/document#declares"] || []).map((declaration) => {
            return new DocumentDeclaration(
                declaration["@id"],
                declaration,
                nestedLabel(docId, declaration["@id"]),
                (declaration["@type"] || [])[0],
                this.extractTag("is-trait-tag", declaration)
            );
        });
        const references = this.extractReferences(doc);
        acc.documents.push(new Document(docId, label(docId), references, declares, this.extractEncodedElement(doc)));
    }

    parseFragment(doc: any, acc) {
        const references = this.extractReferences(doc);
        acc.fragments.push(new Fragment(doc["@id"], label(doc["@id"]), references, this.extractEncodedElement(doc)));
    }

    parseModule(doc: any, acc) {
        const docId = doc["@id"];
        const declares = (doc["http://raml.org/vocabularies/document#declares"] || []).map((declaration) => {
            return new DocumentDeclaration(
                declaration["@id"],
                declaration,
                nestedLabel(docId, declaration["@id"]),
                (declaration["@type"] || [])[0],
                this.extractTag("is-trait-tag", declaration)
            );
        });
        const references = this.extractReferences(doc);
        acc.modules.push(new Module(docId, label(docId), references, declares));
    }

    flatten(array, mutable) {
        const toString = Object.prototype.toString;
        const arrayTypeStr = '[object Array]';

        const result: any[] = [];
        const nodes = (mutable && array) || array.slice();
        let node: any;

        if (!array.length) {
            return result;
        }

        node = nodes.pop();

        do {
            if (toString.call(node) === arrayTypeStr) {
                nodes.push.apply(nodes, node);
            } else {
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
                    return tag["http://raml.org/vocabularies/document#tagId"][0]["@value"] === tagId
                })[0];
            if (tagFound) {
                return tagFound["http://raml.org/vocabularies/document#tagValue"][0]["@value"];
            } else {
                return undefined;
            }
        } catch (e) {
            return undefined;
        }
    }


    extractEncodedElement(node: any) {
        const encoded = (node["http://raml.org/vocabularies/document#encodes"] || [])[0];
        if (encoded != null) {
            const isAbstract = encoded["@type"].find(t => t === "http://raml.org/vocabularies/document#AbstractDomainElement");
            return new DomainElement(encoded["@id"], encoded, nestedLabel(node["@id"], encoded["@id"]), encoded["@type"][0], isAbstract, );
        } else {
            return undefined;
        }
    }

    extractReferences(doc: any) {
        const references = (doc["http://raml.org/vocabularies/document#references"] || []);
        return references.map((ref) => ref["@id"]);
    }

    private foldReferences(acc: { documents: Document[]; fragments: Fragment[]; modules: Module[] }) {
        const mapping: { [id: string]: DocumentId } = {};
        acc.documents.forEach(doc => mapping[doc.id] = doc);
        acc.fragments.forEach(doc => mapping[doc.id] = doc);
        acc.modules.forEach(doc => mapping[doc.id] = doc);

        acc.documents.forEach((doc: Document) => {
            const references = doc.references.map((ref) => mapping[ref as string]);
            doc.references = references;
        });
        acc.fragments.forEach((doc: Document) => {
            const references = doc.references.map((ref) => mapping[ref as string]);
            doc.references = references;
        });
        acc.modules.forEach((doc: Document) => {
            const references = doc.references.map((ref) => mapping[ref as string]);
            doc.references = references;
        });
    }
}
