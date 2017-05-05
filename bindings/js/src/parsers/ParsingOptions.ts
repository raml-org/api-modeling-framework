/**
 * Created by antoniogarrote on 05/05/2017.
 */

/**
 * Parsing options for parsing
 */
interface ParsingOptions {

    /**
     * Sets a mapping from URL prefixes for references to local directories to resolve references in the syntax
     * @param uriToDirs
     */
    cacheDirs?: {[url: string]: string};
}