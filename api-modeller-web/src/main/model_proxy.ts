import {ModelType} from "./api_modeller_window";
import * as jsonld from "jsonld";
import {UnitModel} from "./units_model";
import {LexicalInfo} from "./model_utils";
import {LexicalInfoGenerator} from "./lexical_info_generator";

const apiFramework = window["api_modelling_framework"].core;


export type ModelLevel = "document" | "domain";

const ramlGenerator = new apiFramework.__GT_RAMLGenerator();
const openAPIGenerator = new apiFramework.__GT_OpenAPIGenerator();
const apiModelGenerator = new apiFramework.__GT_APIModelGenerator();

const ramlParser = new apiFramework.__GT_RAMLParser();
const openAPIParser = new apiFramework.__GT_OpenAPIParser();
const apiModelParser = new apiFramework.__GT_APIModelParser();

function from_clj(x: any) {
    return apiFramework.fromClj(x)
}

function to_clj(x: any) {
    try {
        return apiFramework.toClj(x)
    } catch (e) {
        console.log("ERROR");
        console.log(e);
        return x;
    }
}

/**
 * A proxy class to interact with the clojure code containing the logic to interact with a API Model
 */
export class ModelProxy {
    // holders for the generated strings
    public ramlString: string = "";
    public generatedRamlModel: any;
    public openAPIString: string = "";
    public generatedOpenAPIModel: any;
    public apiModelString: string = "";
    public generatedAPIModel: any;
    public lexicalInfoGenerator: LexicalInfoGenerator;

    constructor(public raw: any, public sourceType: ModelType) {
        // we setup the default model with the value passed in the constructor
        // for the kind of model.
        if (this.sourceType === "raml") {
            this.generatedRamlModel = raw;
        } else if(this.sourceType === "open-api") {
            this.generatedOpenAPIModel = raw;
        }

        this.lexicalInfoGenerator = new LexicalInfoGenerator(this);
    }
    location(): string { return apiFramework.location(this.raw) }
    documentModel() { return apiFramework.document_model(this.raw); }
    domainModel() { return apiFramework.domain_model(this.raw); }
    text(): string { return apiFramework.raw(this.raw) }

    /**
     * Serialises the model as RAML/YAML document for the provided document level
     * @param level: document, domain
     * @param cb
     */
    toRaml(level: ModelLevel, options: any, cb) {
        console.log(`** Generating RAML with level ${level}`);
        this.lexicalInfoGenerator.generateLexicalInfo("raml", level, (err, res) => {
            if (err) {
                cb(err, null);
            } else {
                this.ramlString = this.lexicalInfoGenerator.getText("raml", level);
                cb(null, this.ramlString);
            }
        });
    }

    /**
     * Serialised the model as a OpenAPI/JSON document for the provided doucmnet level
     * @param level: document, domain
     * @param cb
     */
    toOpenAPI(level: ModelLevel, options: any, cb) {
        console.log(`** Generating OpenAPI with level ${level}`);
        this.lexicalInfoGenerator.generateLexicalInfo("open-api", level, (err, res) => {
            if (err) {
                cb(err, null);
            } else {
                this.openAPIString = this.lexicalInfoGenerator.getText("open-api", level);
                cb(null, this.openAPIString);
            }
        });
    }

    update(location: string, text: string, cb: (e: any) => void): void {
        console.log("*** TRYING TO RUN THE UPDATE FOR " + location );
        apiFramework.update_reference_model(this.raw, this.location(), this.sourceType, text, (e, newRaw) => {
            if (e != null) {
                cb(e);
            } else {
                this.raw = newRaw;
            }
        });
    }

    toAPIModel(level: ModelLevel, options: any, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        this.toAPIModelProcessed(level, true, true, options, cb);
    }

    toAPIModelProcessed(level: ModelLevel, compacted: boolean, stringify: boolean, options: any, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(
            apiModelGenerator,
            this.location(),
            liftedModel,
            to_clj(options),
            (err, res) => {
                if (err != null) {
                    cb(err, res);
                } else {

                    const parsed = JSON.parse(res);
                    if ( compacted ) {
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
                            if ( stringify ) {
                                this.apiModelString = JSON.stringify(finalJson, null, 2);
                                cb(err, this.apiModelString);
                            } else {
                                cb(err, finalJson);
                            }
                        });
                    } else {
                        if ( stringify ) {
                            this.apiModelString = JSON.stringify(parsed, null, 2);
                            cb(err, this.apiModelString);
                        } else {
                            cb(err, parsed)
                        }

                    }
                }
            });
    }


    public units(modelLevel: ModelLevel, cb) { new UnitModel(this).process(modelLevel, cb); }

    /**
     * Returns all the files referenced in a document model
     * @returns {string[]}
     */
    references(): string[] {
        const files: string[] = [];
        files.push(this.location());
        apiFramework.references(this.raw).forEach(f => files.push(f));
        return files;
    }

    nestedModel(location: string): ModelProxy {
        const rawModel = apiFramework.reference_model(this.raw, location);
        return new ModelProxy(rawModel, this.sourceType);
    }

    findElement(level: ModelLevel, id: string): ModelProxy | undefined {
        const rawModel = apiFramework.find_element(this.raw, level, id);
        if (rawModel) {
            return new ModelProxy(rawModel, this.sourceType);
        } else {
            return undefined;
        }
    }

    elementLexicalInfo(id: string): LexicalInfo | undefined {
        console.log("*** Looking for lexical information about " + id);
        const res = apiFramework.lexical_info_for_unit(this.raw, id);
        if (res != null) {
            return new LexicalInfo(
                parseInt(res["start-line"]),
                parseInt(res["start-column"]),
                parseInt(res["start-index"]),
                parseInt(res["end-line"]),
                parseInt(res["end-column"]),
                parseInt(res["end-index"])
            );
        } else {
            return undefined;
        }
    }

    elementLexicalInfoFor(id: string, model: "raml" | "open-api" | "jsonld", level: ModelLevel, cb:(err: any, res: LexicalInfo | undefined) => void) {
        console.log("*** Looking for lexical information about " + id + " for model " + model);
        var syntax =  null;
        if (model === "raml") {
            syntax = "raml";
        } else if (model === "open-api") {
            syntax = "open-api"
        } else if (model === "jsonld") {
            cb(null, undefined);
        }
        this.lexicalInfoGenerator.lexicalInfoFor(id, syntax, level, cb);
    }
}