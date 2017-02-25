import {ModelProxy, ModelLevel} from "./model_proxy";
import {label, nestedLabel} from "../utils";

export interface DocumentId {
    id: string;
    label: string;
}

export class DomainElement implements DocumentId {
    constructor(public id: string, public label: string, public elementClass: string, isAbstract: boolean) {}
}

export class DocumentDeclaration implements DocumentId {
    constructor(public id: string, public label: string, public elementClass: string, public traitAlias?: string) {}
}

export class Document implements DocumentId {
    constructor(public id: string, public label: string, public declares: DocumentDeclaration[], public encodes?: DomainElement) {}
}

export class Fragment implements DocumentId {
    constructor(public id: string, public label: string, public encodes?: DomainElement) {}
}

export class Module implements DocumentId {
    constructor(public id: string, public label: string, public declares: DocumentDeclaration[]) {}
}

async function consumePromises<T>(ps: (Promise<T>|null)[]) {
    if (ps.length > 0) {
        const next = ps.pop();
        if (next != null) { await next }
        await consumePromises(ps)
    }
}

export class UnitModel {
    constructor(public model: ModelProxy) {}

    public process(modelLevel: ModelLevel, cb) {
        const acc = { "documents": [], "fragments": [], "modules": []};
        const references = this.model.references();
        const promises = references.map(reference => {
            if (modelLevel == "document" || reference == this.model.location()) {
                return new Promise((resolve, reject) => {
                    let nestedModel = this.model.nestedModel(reference);
                    console.log("SENDING SORUCE MAPS TRUE!!!!!");
                    nestedModel.toAPIModelProcessed(modelLevel, false, false, {"source-maps?": true}, (err, jsonld: any) => {
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
                });
            } else {
                return null;
            }
        });

        consumePromises(promises)
            .then((_) => {
                cb(null, acc);
            }).catch((e) => cb(e, null));
    }

    isDocument(doc: any) {
        return (doc["@type"] || []).filter((type: string) => {
                return type === "http://raml.org/vocabularies/document#Document"
            }).length > 0
    }

    isModule(doc: any) {
        return (doc["@type"] || []).filter((type: string) => {
                return type === "http://raml.org/vocabularies/document#Document"
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
            console.log("DECLARATION");
            console.log(declaration);
            return new DocumentDeclaration(
                declaration["@id"],
                nestedLabel(docId, declaration["@id"]),
                (declaration["@type"]||[])[0],
                this.extractTag("is-trait-tag", declaration)
            );
        });
        acc.documents.push(new Document(docId, label(docId), declares, this.extractEncodedElement(doc)));
    }

    parseFragment(doc:any, acc) {
        acc.fragments.push(new Fragment(doc["@id"], label(doc["@id"]), this.extractEncodedElement(doc)));
    }

    parseModule(doc:any, acc) {
        const docId = doc["@id"];
        const declares = (doc["http://raml.org/vocabularies/document#declares"] || []).map((declaration) => {
            return new DocumentDeclaration(
                declaration["@id"],
                nestedLabel(docId, declaration["@id"]),
                (declaration["@type"]||[])[0],
                this.extractTag("is-trait-tag", declaration)
            );
        });
        acc.modules.push(new Module(docId, label(docId), declares));
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
        } catch(e) {
            return undefined;
        }
    }


    extractEncodedElement(node: any) {
        const encoded = (node["http://raml.org/vocabularies/document#encodes"]||[])[0];
        if (encoded != null) {
            const isAbstract = encoded["@type"].find(t => t === "http://raml.org/vocabularies/document#AbstractDomainElement");
            return new DomainElement(encoded["@id"], nestedLabel(node["@id"], encoded["@id"]), encoded["@type"][0], isAbstract);
        } else {
            return undefined;
        }
    }
}