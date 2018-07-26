/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DocumentModel} from "../core/document/DocumentModel";
import {GenerationOptions} from "./GenerationOptions";
import {Clojure} from "../Clojure";

/**
 * Basic interface for all AMF generators. It allows to generate syntax files out of AMF Document Models.
 */
export abstract class BaseGenerator {

    /**
     * Serialises the model and stores it in the provided file path and options
     * @param path Path where the model will be serialised
     * @param model DocumentModel to be serialised
     * @param options Generation options
     * @param cb Callback that will be invoked when the generation has finished reporting potential errors
     */
    public generateFile(model: DocumentModel, path: string, options: GenerationOptions = {}, cb:(e?: Error) => void) {
        Clojure.amf.generate_file(this.generator(), path, model.clojureModel(), options, (e) => {
            if (e != null) {
                cb(e);
            } else {
                cb(null);
            }
        });
    }

    /**
     * Serialises the model and uses the provided file path as the default model location, applying the provided options
     * @param path Path where the model will be serialised
     * @param model DocumentModel to be serialised
     * @param options Geneartion otions
     * @param cb Callback that will be invoked when the generation has finished reporting potential errors and returning
     * the generated text
     */
    public generateString(model: DocumentModel, path: string | null = null, options: GenerationOptions = {}, cb:(e?: Error, text?: string) => void) {
        Clojure.amf.generate_string(this.generator(), path || model.location(), model.clojureModel(), options, (e, result) => {
            if (e != null) {
                cb(e, null);
            } else {
                cb(null, result);
            }
        });
    }

    protected abstract generator(): any;
}