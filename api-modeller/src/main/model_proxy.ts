import {ModelType} from "./api_modeller_window";
require("api_modelling_framework");

const apiFramework = global["api_modelling_framework"].core;
const apiFrameworDocumentModel = global["api_modelling_framework"].model.document;

export type ModelLevel = "document" | "domain";

const ramlGenerator = new apiFramework.RAMLGenerator();
const openAPIGenerator = new apiFramework.OpenAPIGenerator();

/**
 * A proxy class to interact with the clojure code containing the logic to interact with a API Model
 */
export class ModelProxy {
    // holders for the generated strings
    public ramlString: string = "";
    public openAPIString: string = "";

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