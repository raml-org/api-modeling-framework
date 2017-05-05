# AMF Java Programming Guide

Java wrapping code for the AMF library can be found in the `org.raml.amf` namespace.
This package provides a Java friendly interface on top of the native Clojure code of the library.

## Compiling

At this moment we don't provide any artifacts in any repository to use AMF or these Java bindings, so they must be built manually with the help of Leiningen.
In order to build the library, you fist need to build an 'ubejar' with AMF. To accomplish this, from the main AMF project directory run the following instruction:

``` bash
$ lein uberjar
```
After Leiningen has finished you should have a standalone jar for AMF and all its dependencies in the `target/api-modeling-framework-0.1.2-SNAPSHOT-standalone.jar` location. Version might be different from the one at the moment of writing this documentation.

Once the jar for AMF has been generated, we can generate the Java bindings jar. For that change to the `bindings/java` directory in the AMF project and use maven:

``` bash
mvn package
```

After Maven has finished you should have an additional jar in the `target/amf-java-0.1.2-SNAPSHOT.jar` location with the Java bindings. Add both jars into your project to use the AMF Java bindings.

## Parsing

Parsers can be found in the `org.raml.amf.parsers` package of the project. They can be build using the factories in `org.raml.amf.AMF`

```java
DocumentModel model = AMF.RAMLParser().parseFile(new URL("http://test.com/worldmusic/api.raml"));
```

Parsers are include for RAML, OpenAPI and the JSON-LD serialisation of the AMF model.

Parsers can accept options, including a hash-map of URLs to local directories that will be used to resolve references in the parsed documents.

For instance, in the next snippet all remote references to the URLs prefixed by `http://test.com/worldmusic` will be resolved looking into the local directory `/Users/antoniogarrote/world-music-api`.

```java
HashMap<String,String> cacheDirs = new HashMap<>();
cacheDirs.put("http://test.com/worldmusic","/Users/antoniogarrote/vocabs/world-music-api");
ParsingOptions options = new ParsingOptions().setCacheDirs(cacheDirs);

DocumentDocument model = (Document) AMF.RAMLParser().parseFile(new URL("http://test.com/worldmusic/api.raml"), options);
```
The original parsed text can be retrieved using the `rawText` method.

## Navigating the Document Model
The parsing process will return an instance of one of the subclasses of `DocumentModel`.
Depending on what is the parsed file, a `Document`, a `Fragment` or a `Module` instance will be returned.

No matter what is the actual Document Model class, the returned model will also include references to all linked documents in the model.

These references can be listed using the `references` method, and new instances of `DocumentModel` can be built for these references using the `modelForReference` method:

```java
for (URL ref : model.references()) {
  DocumentModel refModel = model.modelForReference(ref);
  System.out.println("Found a reference model: " + refModel);
}
```

## Applying resolution

To run the resolution algorithm and combine all the documents from the Document Model into a single Domain Model description, the method `resolve` can be invoked.

```java
DocumentModel resolvedModel = model.resolve();
```

## Accessing the Domain Model

The parsed Domain Model can be retrieved from the Document Model instance using the appropriate accessor.

Fragments return the encoded Domain Model element using the `encodes` method from the `org.raml.amf.core.document.EncodesDomainModel` interface.
Modules returns the list of declared Domain Model elements using the `declares` method from the `org.raml.amf.core.document.DeclaresDomainModel` interface.
Documents can use both methods to retrieve the top level encoded element and the list of declared elements in the root element.

```java
if (model instanceof EncodesDomainModel) {
  System.out.println(model.encodes());
}

if (targetModel instanceof DeclaresDomainModel) {
  List<DomainModel> declarations = model.declares();
  for(DomainModel decl : declarations) {
    System.out.println(decl);
  }
}
```

## Navigating and mutating the Domain Model

The Domain Model includes Java bean classes for all elements in the AMF Domain Model.
These getters and setters can be used to navigate and mutate the model. Please, refer to the [documentation](https://raml-org.github.io/api-modeling-framework/doc/java/apidocs/index.html) for more details.

```java
APIDocumentation api = (APIDocumentation) model.encodes();

for (EndPoint endpoint : api.getEndpoints()) {
  endpoint.setName("Modified " + endpoint.getName());
}
```

## Serialisation

AMF includes generators capable of serialising the AMF model back into one of the supported syntaxes. The method `generateString` can be used to generate a String representation, and the method `generateFile` can be used to dump the serialised model directly into a file.
Factory methods for each generator can be found in the `org.raml.amf.AMF` class.


```java
// Generating RAML
// Generate can accept just the model
String generated = AMF.RAMLGenerator().generateString(targetModel);
System.out.println(generated);

// Generating OpenAPI
// It can also accept a destination File/URL for the model
generated = AMF.OpenAPIGenerator().generateString(
        new File("world_music.json"),
        targetModel
);
System.out.println(generated);

// Generating JSON-LD
// Finally it can also accept a set of generation options
generated = AMF.JSONLDGenerator().generateString(
        new File("world_music.jsonld"),
        targetModel,
        new GenerationOptions()
                .setFullgraph(true)
                .setSourceMapGeneration(true));
System.out.println(generated);
```

Two options are available when generating JSON-LD documents.
- `setFullGraph` will nest the JSON-LD graphs for the referenced documents in the model to be serialised, otherwise only URIs will be generated.
- `setSourceMapGeneration` enables or disables the generation of source maps JSON-LD information in the output.
