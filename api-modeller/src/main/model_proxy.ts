import {ModelType} from "./api_modeller_window";
import * as jsonld from "jsonld";
import {JsonLd} from "jsonld";
import {stringify} from "querystring";
import {UnitModel} from "./units_model";
require("api_modelling_framework");

const apiFramework = global["api_modelling_framework"].core;
const apiFrameworDocumentModel = global["api_modelling_framework"].model.document;

export type ModelLevel = "document" | "domain";

const ramlGenerator = new apiFramework.RAMLGenerator();
const openAPIGenerator = new apiFramework.OpenAPIGenerator();
const apiModelGenerator = new apiFramework.APIModelGenerator();
/**
 * A proxy class to interact with the clojure code containing the logic to interact with a API Model
 */
export class ModelProxy {
    // holders for the generated strings
    public ramlString: string = "";
    public openAPIString: string = "";
    public apiModeltring: string = "";

    constructor(public raw: any, public sourceType: ModelType) {}
    location(): string { return apiFramework.location(this.raw) }
    documentModel() { return apiFramework.document_model(this.raw); }
    domainModel() { return apiFramework.domain_model(this.raw); }

    /**
     * Serialises the model as RAML/YAML document for the provided document level
     * @param level: document, domain
     * @param cb
     */
    toRaml(level: ModelLevel, cb) {
        console.log(`** Generating RAML with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(
            ramlGenerator,
            this.location(),
            liftedModel,
            {},
            (err, res) => {
                console.log("BACK!");
                console.log(err);
                if (err != null) {
                    cb(err,res);
                } else {
                    this.ramlString = res;
                    cb(err,res)
                }
            }
        );
    }

    /**
     * Serialised the model as a OpenAPI/JSON document for the provided doucmnet level
     * @param level: document, domain
     * @param cb
     */
    toOpenAPI(level: ModelLevel, cb) {
        console.log(`** Generating OpenAPI with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(
            openAPIGenerator,
            this.location(),
            liftedModel,
            {},
            (err, res) => {
                if (err != null) {
                    cb(err, res);
                } else {
                    this.openAPIString = JSON.stringify(JSON.parse(res), null, 2);
                    cb(err, this.openAPIString);
                }
            });
    }

    toAPIModel(level: ModelLevel, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        this.toAPIModelProcessed(level, true, true, cb);
    }

    toAPIModelProcessed(level: ModelLevel, compacted: boolean, stringify: boolean, cb) {
        console.log(`** Generating API Model JSON-LD with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(
            apiModelGenerator,
            this.location(),
            liftedModel,
            {},
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
                                this.apiModeltring = JSON.stringify(finalJson, null, 2);
                                cb(err, this.apiModeltring);
                            } else {
                                cb(err, finalJson)
                            }
                        });
                    } else {
                        if ( stringify ) {
                            this.apiModeltring = JSON.stringify(parsed, null, 2);
                            cb(err, this.apiModeltring);
                        } else {
                            cb(err, parsed)
                        }

                    }
                }
            });
    }


    public units(cb) { new UnitModel(this).process(cb); }

    /**
     * Returns all the files referenced in a document model
     * @returns {string[]}
     */
    references(): string[] {
        const files: string[] = [];
        files.push(this.location());
        const references = apiFramework.from_clj(apiFrameworDocumentModel.references(this.domainModel()));
        references.forEach(ref => files.push(ref.location));
        return files;
    }

    nestedModel(location: string): ModelProxy {
        const rawModel = apiFramework.reference_model(this.raw, location);
        return new ModelProxy(rawModel, this.sourceType);
    }
}