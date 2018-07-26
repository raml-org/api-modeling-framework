/**
 * Created by antoniogarrote on 05/05/2017.
 */

/**
 * Options for model serialisation
 */
export interface GenerationOptions {
    /**
     * When serialising to JSON-LD, enables or disables the generation of source-maps
     * @param shouldGenerate
     */
    'source-maps?'?: boolean;

    /**
     * When serialising into JSON-LD, if set to true, all the JSON-LD RDF graph for referenced documents will be nested inside
     * the JSON-LD document of the model.
     * If set to false, only the URI will be serialised
     * @param shouldGenerateFullGraph
     */
    'full-graph?'?: boolean;
}