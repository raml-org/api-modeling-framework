"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const rdfstore_1 = require("rdfstore");
const ko = require("knockout");
const domain_model_1 = require("../main/domain_model");
class PredefinedQuery {
    constructor(name, text) {
        this.name = name;
        this.text = text;
    }
}
class Query {
    constructor() {
        this.viewModel = window['viewModel'];
        this.text = ko.observable("SELECT * { ?s ?p ?o }");
        this.variables = ko.observableArray([]);
        this.bindings = ko.observableArray([]);
        this.predefinedQueries = ko.observableArray([
            new PredefinedQuery("All assertions ordered by subject", "SELECT * { ?s ?p ?o } ORDER BY DESC(?s)"),
            new PredefinedQuery("All assertions ordered by predicate", "SELECT * { ?s ?p ?o } ORDER BY DESC(?p)"),
            new PredefinedQuery("All properties in the graph", `SELECT DISTINCT ?property 
 { ?s ?property ?o } 
ORDER BY DESC(?property)`),
            new PredefinedQuery("Graph node types", "SELECT ?type ?node { ?node rdf:type ?type } ORDER BY DESC(?type)"),
            new PredefinedQuery("All Documents in the Document Model", `SELECT * { 
  ?resource rdf:type doc:Unit ;
            rdf:type ?type .
  FILTER (?type != doc:Unit)
} ORDER BY DESC(?s)`),
            new PredefinedQuery("Resources with methods defining successful responses", `SELECT ?path ?method ?status { 
        
   ?resource rdf:type http:EndPoint ;
             http:path ?path ;
             hydra:supportedOperation ?operation .
              
   ?operation hydra:method ?method ;
              hydra:returns ?response .
                 
   ?response hydra:statusCode ?status .
   FILTER (?status = "200"^^xsd:string) .
                 
} ORDER BY DESC(?path)`),
            new PredefinedQuery("Encoded Domain elements", `SELECT * { 
  
  ?document doc:encodes ?domainElement . 
  ?domainElement rdf:type ?type .

  FILTER (?type != doc:DomainElement) .

} ORDER BY DESC(?domainElement)`)
        ]);
        this.selectedPredefinedQuery = ko.observable(undefined);
        this.selectedPredefinedQuery.subscribe((value) => {
            if (value != null) {
                this.text(value.text);
            }
        });
    }
    process(jsonld, cb) {
        console.log("STORING JSONLD DOC");
        new rdfstore_1.Store((err, store) => {
            if (err) {
                alert("Error creating RDF store");
            }
            else {
                this.store = store;
                this.store.registerDefaultProfileNamespaces();
                this.store.registerDefaultNamespace("hydra", domain_model_1.HYDRA_NS);
                this.store.registerDefaultNamespace("doc", domain_model_1.DOCUMENT_NS);
                this.store.registerDefaultNamespace("http", domain_model_1.HTTP_NS);
                this.store.registerDefaultNamespace("shapes", domain_model_1.SHAPES_NS);
                this.store.registerDefaultNamespace("shacl", domain_model_1.SHACL_NS);
                this.store.registerDefaultNamespace("rdf", domain_model_1.RDF_NS);
                this.store.registerDefaultNamespace("rdfs", domain_model_1.RDFS_NS);
                this.store.registerDefaultNamespace("xsd", domain_model_1.XSD_NS);
                this.store.registerDefaultNamespace("schema-org", domain_model_1.SCHEMA_ORG_NS);
                store.load("application/ld+json", jsonld, ((err) => {
                    if (err) {
                        cb(new Error(err), null);
                    }
                    else {
                        cb(null, this);
                    }
                }));
            }
        });
    }
    submit() {
        console.log("SUBMITTING QUERY");
        if (this.store) {
            this.store.execute(this.text(), (err, res) => {
                if (err) {
                    alert("Error executing query " + this.text());
                    console.log(err);
                }
                else {
                    console.log(`Found ${res.length} results`);
                    this.resetVariables(res[0]);
                    this.bindings(res);
                }
            });
        }
    }
    resetVariables(re) {
        this.variables.removeAll();
        for (let p in re) {
            this.variables.push(p);
        }
    }
}
exports.Query = Query;
//# sourceMappingURL=query.js.map