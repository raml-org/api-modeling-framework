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
const jsonld = require("jsonld");
const units_model_1 = require("./units_model");
const model_utils_1 = require("./model_utils");
const apiFramework = global["api_modelling_framework"].core;
const ramlGenerator = new apiFramework.RAMLGenerator();
const openAPIGenerator = new apiFramework.OpenAPIGenerator();
const apiModelGenerator = new apiFramework.APIModelGenerator();
function from_clj(x) {
    return apiFramework.fromClj(x);
}
function to_clj(x) {
    try {
        return apiFramework.toClj(x);
    }
    catch (e) {
        console.log("ERROR");
        console.log(e);
        return x;
    }
}
/**
 * A proxy class to interact with the clojure code containing the logic to interact with a API Model
 */
class ModelProxy {
    constructor(raw, sourceType) {
        this.raw = raw;
        this.sourceType = sourceType;
        // holders for the generated strings
        this.ramlString = "";
        this.openAPIString = "";
        this.apiModeltring = "";
    }
    location() { return apiFramework.location(this.raw); }
    documentModel() { return apiFramework.document_model(this.raw); }
    domainModel() { return apiFramework.domain_model(this.raw); }
    text() { return apiFramework.raw(this.raw); }
    /**
     * Serialises the model as RAML/YAML document for the provided document level
     * @param level: document, domain
     * @param cb
     */
    toRaml(level, options, cb) {
        console.log(`** Generating RAML with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(ramlGenerator, this.location(), liftedModel, {}, (err, res) => {
            if (err != null) {
                cb(err, res);
            }
            else {
                this.ramlString = res;
                cb(err, res);
            }
        });
    }
    /**
     * Serialised the model as a OpenAPI/JSON document for the provided doucmnet level
     * @param level: document, domain
     * @param cb
     */
    toOpenAPI(level, options, cb) {
        console.log(`** Generating OpenAPI with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(openAPIGenerator, this.location(), liftedModel, {}, (err, res) => {
            if (err != null) {
                cb(err, res);
            }
            else {
                this.openAPIString = JSON.stringify(JSON.parse(res), null, 2);
                cb(err, this.openAPIString);
            }
        });
    }
    update(location, text) {
        return __awaiter(this, void 0, void 0, function* () {
            return new Promise((resolve, reject) => {
                console.log("*** TRYING TO RUN THE UPDATE FOR " + location);
                apiFramework.update_reference_model(this.raw, this.location(), this.sourceType, text, (e, newRaw) => {
                    if (e != null) {
                        reject(e);
                    }
                    else {
                        this.raw = newRaw;
                        resolve();
                    }
                });
            });
        });
    }
    toAPIModel(level, options, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        this.toAPIModelProcessed(level, true, true, options, cb);
    }
    toAPIModelProcessed(level, compacted, stringify, options, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(apiModelGenerator, this.location(), liftedModel, to_clj(options), (err, res) => {
            if (err != null) {
                cb(err, res);
            }
            else {
                const parsed = JSON.parse(res);
                if (compacted) {
                    const context = {
                        "raml-doc": "http://raml.org/vocabularies/document#",
                        "raml-http": "http://raml.org/vocabularies/http#",
                        "raml-shapes": "http://raml.org/vocabularies/shapes#",
                        "hydra": "http://www.w3.org/ns/hydra/core#",
                        "shacl": "http://www.w3.org/ns/shacl#",
                        "schema-org": "http://schema.org/",
                        "xsd": "http://www.w3.org/2001/XMLSchema#"
                    };
                    jsonld.compact(parsed, context, (err, compacted) => {
                        if (err != null) {
                            console.log("ERROR COMPACTING");
                            console.log(err);
                        }
                        const finalJson = (err == null) ? compacted : parsed;
                        if (stringify) {
                            this.apiModeltring = JSON.stringify(finalJson, null, 2);
                            cb(err, this.apiModeltring);
                        }
                        else {
                            cb(err, finalJson);
                        }
                    });
                }
                else {
                    if (stringify) {
                        this.apiModeltring = JSON.stringify(parsed, null, 2);
                        cb(err, this.apiModeltring);
                    }
                    else {
                        cb(err, parsed);
                    }
                }
            }
        });
    }
    units(modelLevel, cb) { new units_model_1.UnitModel(this).process(modelLevel, cb); }
    /**
     * Returns all the files referenced in a document model
     * @returns {string[]}
     */
    references() {
        const files = [];
        files.push(this.location());
        from_clj(apiFramework.references(this.raw).forEach(f => files.push(f)));
        return files;
    }
    nestedModel(location) {
        const rawModel = apiFramework.reference_model(this.raw, location);
        return new ModelProxy(rawModel, this.sourceType);
    }
    findElement(level, id) {
        const rawModel = apiFramework.find_element(this.raw, level, id);
        if (rawModel) {
            return new ModelProxy(rawModel, this.sourceType);
        }
        else {
            return undefined;
        }
    }
    elementLexicalInfo(id) {
        console.log("*** Looking for lexical information about " + id);
        const res = apiFramework.lexical_info_for_unit(this.raw, id);
        return new model_utils_1.LexicalInfo(parseInt(res["start-line"]), parseInt(res["start-column"]), parseInt(res["start-index"]), parseInt(res["end-line"]), parseInt(res["end-column"]), parseInt(res["end-index"]));
    }
}
exports.ModelProxy = ModelProxy;
//# sourceMappingURL=model_proxy.js.map