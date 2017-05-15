package org.raml.amf.examples;

import org.raml.amf.AMF;
import org.raml.amf.core.document.Document;
import org.raml.amf.core.document.DocumentModel;
import org.raml.amf.core.document.EncodesDomainModel;
import org.raml.amf.core.document.Module;
import org.raml.amf.core.domain.APIDocumentation;
import org.raml.amf.core.domain.DomainModel;
import org.raml.amf.core.domain.EndPoint;
import org.raml.amf.core.domain.Type;
import org.raml.amf.core.domain.shapes.NodeShape;
import org.raml.amf.core.domain.shapes.PropertyShape;
import org.raml.amf.core.domain.shapes.Shape;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.core.exceptions.ResolutionException;
import org.raml.amf.core.exceptions.UnknownModelReferenceException;
import org.raml.amf.generators.*;
import org.raml.amf.parsers.ParsingException;
import org.raml.amf.parsers.ParsingOptions;
import org.raml.amf.parsers.RAMLParser;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */
public class BasicParsingAndNavigation {

    public static void main(String[] args) throws MalformedURLException, ParsingException, InvalidModelException, UnknownModelReferenceException, GenerationException, ResolutionException {

        URL toParse = new URL("file:///Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/world-music-api/api.raml");
        // Model model = new RAMLParser().parseFile();
        HashMap<String,String> cacheDirs = new HashMap<>();
        cacheDirs.put("http://test.com/something","/Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/world-music-api");
        ParsingOptions options = new ParsingOptions().setCacheDirs(cacheDirs);
        Document model = (Document) AMF.RAMLParser().parseFile(new URL("http://test.com/something/api.raml"), options);
        System.out.println("GOT A MODEL");
        System.out.println(model);

        System.out.println("LOCATION: " + model.location());
        for (URL ref : model.references()) {
            System.out.println("REFERENCE: " + ref);
        }


        toParse = new URL("file:///Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/world-music-api/api.raml");
        model = (Document) new RAMLParser().parseFile(toParse);
        System.out.println("GOT A MODEL");
        System.out.println(model);

        DocumentModel resolvedModel = model.resolve();
        System.out.println("RESOLVED");
        System.out.println(AMF.RAMLGenerator().generateString(resolvedModel));

        System.out.println("LOCATION: " + model.location());
        for (URL ref : model.references()) {
            System.out.println("REFERENCE: " + ref);
        }

        System.out.println(model.rawText().get());

        URL targetRef = model.references()[1];
        System.out.println("TARGETTING " + targetRef);
        DocumentModel targetModel = model.modelForReference(targetRef);
        System.out.println("TARGET LOCATION: " + targetModel.location());
        System.out.println("TARGET MODEL CLASS " + targetModel.getClass());
        Module module = null;
        for(URL ref : model.references()) {
            DocumentModel doc = model.modelForReference(ref);
            if (doc instanceof Module) {
                module = (Module) doc;
                break;
            }
        }
        List<DomainModel> declarations = module.declares();
        System.out.println("DECLARATIONS:");

        System.out.println("SHAPES:");
        int shapesFound = 0;
        for(DomainModel decl : declarations) {
            System.out.println(decl);
            if (decl instanceof Type) {
                shapesFound++;
                Shape shape = ((Type) decl).getShape();
                System.out.println("SHAPE " + shape.getId());
                if (shape instanceof NodeShape) {
                    List<PropertyShape> properties = ((NodeShape) shape).getPropertyShapes();
                    for (PropertyShape prop : properties) {
                        if (prop.getDatatype() != null) {
                            System.out.println("DATATYPE " + prop.getDatatype());
                        } else if(prop.getNode() != null) {
                            System.out.println("NESTED SHAPE " + prop.getNode().getId());
                        } else {
                            // throw new RuntimeException("Property shape without data type or nested shape");
                        }
                    }
                }
            }
        }
        System.out.println("NUMBER OF SHAPES? " + shapesFound);


        Object foundModel = targetModel.findDomainElement("/Users/antoniogarrote/Development/api-modelling-framework/resources/other-examples/world-music-api/libraries/api.lib.raml#/definitions/Cat");
        System.out.println("FOUND?");
        System.out.println(foundModel);

        APIDocumentation api = (APIDocumentation) model.encodes();

        System.out.println("API DOCUMENTATION " + api.getName());

        System.out.println("ENDPOINTS " + api.getEndpoints());
        for (EndPoint endpoint : api.getEndpoints()) {
            endpoint.setName("Modified " + endpoint.getName());
            System.out.println(endpoint.getName());
        }

        String generated = AMF.OpenAPIGenerator().generateString(targetModel);
        System.out.println("GENERATED OpenAPI");
        System.out.println(generated);

        generated = AMF.RAMLGenerator().generateString(
                new File("world_music.raml"),
                targetModel
        );
        System.out.println("GENERATED RAML");
        System.out.println(generated);

        generated = AMF.JSONLDGenerator().generateString(
                new File("world_music.jsonld"),
                targetModel,
                new GenerationOptions()
                        .setFullgraph(true)
                        .setSourceMapGeneration(true)
        );
        System.out.println("GENERATED JSONLD");
        System.out.println(generated);
    }
}
