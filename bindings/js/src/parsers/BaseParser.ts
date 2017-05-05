/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DocumentModel} from "../core/document/DocumentModel";
import {URL} from "../../index";
import {Clojure} from "../Clojure";

/**
 * Basic interface for all AMF parsers. It allows to parse syntax files and syntax text and generate the AMF Model out
 * of it.
 */
export abstract class BaseParser {

    /**
     * Generates a model parsing the file referenced by the provided URL and parsing options.
     * @param url Local or remote URL
     * @param options Parsing options
     * @param cb Callback that will receive either the parsed model or error
     * @return The parsed model
     */
    public parseFile(url: URL, options: ParsingOptions = {}, cb: (err?: Error, model?: DocumentModel) => void)  {
        Clojure.amf.parse_file(this.parser(), url, Clojure.jsToCljs(options), (err, model) => {
            if (err) {
                cb(err)
            } else {
                cb(undefined, DocumentModel.fromRawModel(model));
            }
        });
    }

    /**
     * Generates a model parsing the provided textual input syntax and parsing options.
     * @param text Input syntax to parse
     * @param url Base URL for the document being parsed. It will be the base URL for the inclusions in this file
     * @param options Parsing options
     * @param cb callback that will receive either the parsed model or error
     * @return The parsed Model
     */
    public parseString(text: string, url: URL, options: ParsingOptions = {}, cb: (err?: Error, model?: DocumentModel) => void)  {
        Clojure.amf.parse_string(this.parser(), url, Clojure.jsToCljs(options), (err, model) => {
            if (err) {
                cb(err)
            } else {
                cb(undefined, DocumentModel.fromRawModel(model));
            }
        });
    }


    protected abstract parser(): any;
}