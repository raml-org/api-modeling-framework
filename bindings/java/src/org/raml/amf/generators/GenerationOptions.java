package org.raml.amf.generators;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


public class GenerationOptions {

    private Boolean generateSourceMaps;
    private Boolean generateFullGraph;

    /**
     * When serialising to JSON-LD, enables or disables the generation of source-maps
     * @param shouldGenerate
     */
    public GenerationOptions setSourceMapGeneration(Boolean shouldGenerate) {
        this.generateSourceMaps = shouldGenerate;
        return this;
    }

    /**
     * When serialising into JSON-LD, if set to true, all the JSON-LD RDF graph for referenced documents will be nested inside
     * the JSON-LD document of the model.
     * If set to false, only the URI will be serialised
     * @param shouldGenerateFullGraph
     */
    public GenerationOptions setFullgraph(Boolean shouldGenerateFullGraph) {
        this.generateFullGraph = shouldGenerateFullGraph;
        return this;
    }

    public IPersistentMap build() {
        IPersistentMap options = PersistentHashMap.EMPTY;
        if (this.generateSourceMaps != null) {
            options = options.assoc("source-maps?", this.generateSourceMaps);
        }
        if (this.generateFullGraph != null) {
            options = options.assoc("full-graph?", this.generateSourceMaps);
        }

        return options;
    }
}
