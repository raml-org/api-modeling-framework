"use strict";
require("api_modelling_framework");
const apiFramework = global["api_modelling_framework"].core;
const apiFrameworDocumentModel = global["api_modelling_framework"].model.document;
const ramlGenerator = new apiFramework.RAMLGenerator();
const openAPIGenerator = new apiFramework.OpenAPIGenerator();
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
    }
    location() { return apiFramework.location(this.raw); }
    documentModel() { return apiFramework.document_model(this.raw); }
    domainModel() { return apiFramework.domain_model(this.raw); }
    /**
     * Serialises the model as RAML/YAML document for the provided document level
     * @param level: document, domain
     * @param cb
     */
    toRaml(level, cb) {
        console.log(`** Generating RAML with level ${level}`);
        let liftedModel = (level === "document") ? this.documentModel() : this.domainModel();
        apiFramework.generate_string(ramlGenerator, this.location(), liftedModel, {}, (err, res) => {
            console.log("BACK!");
            console.log(err);
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
    toOpenAPI(level, cb) {
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
    /**
     * Returns all the files referenced in a document model
     * @returns {string[]}
     */
    references() {
        const files = [];
        files.push(this.location());
        const references = apiFramework.from_clj(apiFrameworDocumentModel.references(this.domainModel()));
        references.forEach(ref => files.push(ref.location));
        return files;
    }
}
exports.ModelProxy = ModelProxy;
//# sourceMappingURL=model_proxy.js.map