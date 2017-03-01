declare module "rdfstore" {

    export interface RdfValue {
        token: string;
        value: any;
        type?: string;
        lang?: string;
    }

    export type Bindings = { [variable:string]: RdfValue };
    export type SelectResults = Bindings[];

    /**
     * Main Store class
     */
    export class Store {

        /**
         * Creates a new store.<br/>
         * <br/>
         * It accepts two optional arguments, a map of configuration
         * options for the store and a callback function.<br/>
         *
         * @constructor
         * @param {Object} [params]
         * @param {Function} [callback] Callback that will be invoked when the store has been created
         * <ul>
         *  <li> persistent:  should the store use persistence? </li>
         *  <li> treeOrder: in versions of the store backed by the native indexing system, the order of the BTree indices</li>
         *  <li> name: when using persistence, the name for this store. In the MongoDB backed version, name of the DB used by the store. By default <code>'rdfstore_js'</code> is used</li>
         *  <li> overwrite: clears the persistent storage </li>
         *  <li> maxCacheSize: if using persistence, maximum size of the index cache </li>
         * </ul>
         */
        constructor();
        constructor(callback: (err?:any, store?:Store) => void);
        constructor(options: { [option:string]: any }, callback: (err?:any, store?:Store) => void);

        /**
         * Registers a new function with an associated name that can
         * be invoked as 'custom:fn_name(arg1,arg2,...,argn)' inside
         * a SPARQL query.
         * <br/>
         * The registered function will receive two arguments, an
         * instance of the store's query filters engine and a list
         * with the arguments received by the function in the SPARQL query.
         * <br/>
         * The function must return a single token value that can
         * consist in a literal value or an URI.
         * <br/>
         * The following is an example literal value:
         * {token: 'literal', type:"http://www.w3.org/2001/XMLSchema#integer", value:'3'}
         * This is an example URI value:
         * {token: 'uri', value:'http://test.com/my_uri'}
         * <br/>
         * The query filters engine can be used to perform common operations
         * on the input values.
         * An error can be returne dusing the 'ebvError' function of the engine.
         * True and false values can be built directly using the 'ebvTrue' and
         * 'ebvFalse' functions.
         *
         * A complete reference of the available functions can be found in the
         * documentation or source code of the QueryFilters module.
         *
         * @arguments:
         * @param {String} [name]: name of the custom function, it will be accesible as custom:name in the query
         * @param {Function} [fn]: lambda function with the code for the query custom function.
         */
        registerCustomFunction(name: string, fn: (filterEngine:any, fnArgs:RdfValue[])=> RdfValue)

        /**
         * Executes a query in the store.<br/>
         * <br/>
         * There are two possible ways of invoking this function,
         * providing a pair of arrays of namespaces that will be
         * used to compute the union of the default and named
         * dataset, or without them.
         * <br/>
         * <br/>
         * Both invocations receive a last parameter
         * a callback function that will receive the return status
         * of the query and the results.
         * <br/>
         * <br/>
         * Results can have different formats:
         * <ul>
         *  <li> SELECT queries: array of binding maps </li>
         *  <li> CONSTRUCT queries: RDF JS Interface Graph object </li>
         *  <li> ASK queries: JS boolean value </li>
         *  <li> LOAD/INSERT... queries: Number of triples modified/inserted </li>
         * </ul>
         *
         * @arguments:
         * @param {String} query
         * @param {String} [defaultURIs] default namespaces
         * @param {String} [namespacesURIs] named namespaces
         * @param {Function} [callback]
         */
        execute(query:string, callback: (err:any | null, results: SelectResults | boolean | number) => void)
        execute(query:string,
                defaultGraphs: string[],
                namedGraphs: string[],
                callback: (err?:any, results?:any) => void);


        /**
         * Load triples into a graph in the store. Data can be passed directly to the method
         * or a remote URI speifying where the data is located can be used.<br/>
         *<br/>
         * If the data is passed directly to the load function, the media type stating the format
         * of the data must also be passed to the function.<br/>
         *<br/>
         * If an URI is passed as a parameter, the store will attempt to perform content negotiation
         * with the remote server and get a representation for the RDF data matching one of the
         * the RDF parsers registered in the store. In this case, the media type parameter must be
         * set to the <code>'remote'</code> value.<br/>
         *<br/>
         * An additional URI for the graph where the parsed data will be loaded and a callback function
         * can be also passed as parameters. If no graph is specified, triples will be loaded in the
         * default graph.<br/>
         *<br/>
         * By default loading data will not trigger notification through the events API. If events needs to
         * be trigger, the functio <code>setBatchLoadEvents</code> must be invoked with a true parameter.
         *
         * @arguments
         * @param {String} mediaType Media type (application/json, text/n3...) of the data to be parsed or the value <code>'remote'</code> if a URI for the data is passed instead
         * @param {String} data RDF data to be parsed and loaded or an URI where the data will be retrieved after performing content negotiation
         * @param {String} [graph] Graph where the parsed triples will be inserted. If it is not specified, triples will be loaded in the default graph
         * @param {Function} callback that will be invoked with a success notification and the number of triples loaded.
         */
        load(mediaType:string, data:any, callback:(err:any | null) => void);
        load(mediaType:string, data:any, graph:string, callback:(err:any | null) => void);

        /**
         * Registers standard namespaces according to the RDF-JS Interfaces spec
         */
        registerDefaultProfileNamespaces():void;

        /**
         * Registers a namespace into the store
         */
        registerDefaultNamespace(alias: string, uri: string):void

    }

    export function create(callback: (err?:any, store?:Store) => void): void
    export function create(options: { [option:string]: any }, callback: (err:any | null, store:Store | null) => void): void
}