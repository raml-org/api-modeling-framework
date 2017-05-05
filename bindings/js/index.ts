/**
 * Created by antoniogarrote on 05/05/2017.
 */
import {RAMLParser} from "./src/parsers/RAMLParser";
import {OpenAPIParser} from "./src/parsers/OpenAPIParser";
import {AMFJSONLDParser} from "./src/parsers/AMFJSONLDParser";
import {RAMLGenerator} from "./src/generators/RAMLGenerator";
import {OpenAPIGenerator} from "./src/generators/OpenAPIGenerator";
import {AMFJSONDGenerator} from "./src/generators/AMFJSONLDGenerator";
export type URL = string;

/**
 * Facade class providing access to the main IO facilities in the library
 */
export class AMF {
    /**
     * Builds a RAML to AMF parser
     * @return
     */
    public static RAMLParser = new RAMLParser();

    /**
     * Builds an OpenAPI to AMF parser
     * @return
     */
    public static OpenAPIParser = new OpenAPIParser();

    /**
     * Builds a AMF encoded JSON-LD to AMF parser
     * @return
     */
    public static JSONLDParser = new AMFJSONLDParser();

    /**
     * Builds a AMF to RAML generator
     * @return
     */
    public static RAMLGenerator = new RAMLGenerator();

    /**
     * Builds a AMF to OpenAPI generator
     * @return
     */
    public static OpenAPIGenerator = new OpenAPIGenerator();

    /**
     * Builds a AMF to JSON-LD generator
     * @return
     */
    public static JSONLDGenerator = new AMFJSONDGenerator();
}