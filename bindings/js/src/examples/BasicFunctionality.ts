/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {RAMLParser} from "../parsers/RAMLParser";
import {APIDocumentation} from "../core/domain/APIDocumentation";
import {Clojure} from "../Clojure";
import {EndPoint} from "../core/domain/EndPoint";
import {Module} from "../core/document/Module";
import {Type} from "../core/domain/Type";
import {AMF} from "../../index";

// Parsing
let apiFile = "http://test.com/something/api.raml";
let cacheDirs = {'cacheDirs': {"http://test.com/something":"/Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/world-music-api"}}
AMF.RAMLParser.parseFile(apiFile, cacheDirs, (err, model) => {
   try {
      console.log("BACK FROM PARSING");
      console.log(err == null);
      console.log(model != null);
      console.log(model.constructor["name"]);
      console.log("FINDING REFERENCES");

      // references
      let references = model.references().map(ref => {
         console.log(ref);
         return ref;
      });
      if (references[0] != null) {
         console.log(`FINDING MODEL FOR ${references[0]}`);
         let foundModel = model.modelForReference(references[0]);
         console.log(foundModel != null);
         console.log(`FOUND MODEL WITH LOCATION ${foundModel.location()} FOR REFERENCE ${references[0]}`);
      }

      // encodes
      console.log("RETRIEVING ENCODED ELEMENT");
      let api = model.encodes() as APIDocumentation;
      console.log(api != null);

      // displaying endpoints
      let endpoints = api.getEndPoints();
      console.log("EndPoints");
      console.log("SOMETHING? " + Clojure.amf_document.id(api.clojureModel()));
      console.log(api.getId());
      endpoints.forEach(e => {
         console.log("ONE ENDPOINT");
         console.log(`${e.getId()} => ${e.getPath()}`)
      });

      // updating endponts
      const before = endpoints.length;
      console.log("BEFORE COUNT " + before);
      let newEndpoint = EndPoint.build("http://test.com/external/1");
      newEndpoint.setPath("/lala");
      const afterBreakPoints = endpoints.concat([newEndpoint]);
      console.log("SETTING THE ENDPOINTS");
      api.setEndPoints(afterBreakPoints);

      // displaying updated endpoints
      endpoints = api.getEndPoints();
      endpoints.forEach(e => {
         console.log("ONE ENDPOINT");
         console.log(`${Clojure.cljsToJs(e.getId())} => ${e.getPath()}`)
      });
      const after = endpoints.length;
      console.log("AFTER COUNT " + after);

      // resolution
      const resolvedModel = model.resolve();
      endpoints = (resolvedModel.encodes() as APIDocumentation).getEndPoints();
      console.log("Resolved EndPoints");
      endpoints.forEach(e => {
         console.log("ONE ENDPOINT");
         console.log(`${e.getId()} => ${e.getPath()}`)
      });

      // raw text
      console.log(model.rawText() != null)

      // declarations
      let module = model.references()
          .map(ref => model.modelForReference(ref))
          .filter(m => m instanceof Module)[0] as Module;
      console.log(module != null);

      let declared = module.declares();
      console.log(declared.length === 3);

      // types
      let types = declared.filter(d => d instanceof Type) as Type[];
      console.log(types.length === 3);
      types.forEach(t => console.log(t.getShape() != null));

      // Generations
      AMF.RAMLGenerator.generateString(model, null, null, (e, r) => console.log(r != null));
      AMF.OpenAPIGenerator.generateString(model, null, null, (e, r) => console.log(r != null));
      AMF.JSONLDGenerator.generateString(model, null, {'full-graph?': true, 'source-maps?': true}, (e, r) => r != null);
   } catch (e) {
      console.log("EXCEPTION!!");
      console.log(e);
   }
});