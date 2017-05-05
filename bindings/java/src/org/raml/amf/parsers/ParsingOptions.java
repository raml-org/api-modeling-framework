package org.raml.amf.parsers;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;

import java.util.HashMap;

/**
 * Parsing options for parsing
 */
public class ParsingOptions {
    private IPersistentMap cacheDirs;

    /**
     * Sets a mapping from URL prefixes for references to local directories to resolve references in the syntax
     * @param uriToDirs
     */
    public ParsingOptions setCacheDirs(HashMap<String,String> uriToDirs) {
        this.cacheDirs = PersistentHashMap.EMPTY;
        for (String key : uriToDirs.keySet()) {
            this.cacheDirs = this.cacheDirs.assoc(key, uriToDirs.get(key));
        }
        return this;
    }

    public IPersistentMap build() {
        IPersistentMap options = PersistentHashMap.EMPTY;
        if (this.cacheDirs != null) {
            options = options.assoc("cacheDirs", this.cacheDirs);
        }

        return options;
    }
}
