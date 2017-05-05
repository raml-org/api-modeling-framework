# AMF JS Programming Guide

JavaScript wrapping is available as a node NPM package.
This package provides a JS friendly interface on top of the native ClojureScript code of the library.

## Compiling

At this moment we don't provide any artifacts in any repository to use AMF or these JS bindings, so they must be built manually with the help of Leiningen.
In order to build the NPM package, you first need to generate the node version of the AMF library. This can be accomplished using the following command:

``` bash
$ lein node
```

When the compilation has finished the node package will be available in `output/node`. Next step is linking the package directory so it will be avaliable for NPM.

``` bash
$ cd output/node && npm link
```

Finally we need to compile and link the JS bindings package. From the AMF top level directory execute:

``` bash
$ cd bindings/js
$ npm link api-modeling-framework
$ npm install
$ tsc
$ npm link
```

Now you can go to your NPM project directory and link the AMF JS bindings:

``` bash
$ npm link amf-js
```

If you start a node REPL in your project now you should be able to require the library:

``` javascript
require("amf-js")
```

## Parsing

Parsers can be found in the `parsers` package of the project. They can be build using the factories in the `AMF` class

``` typescript
let apiFile = "file:///path/to/other-examples/world-music-api/api.raml";
AMF.RAMLParser.parseFile(apiFile, {}, (err, model) => {
    if (err != null) {
      console.log(`Created model from file ${model.location()}`);
    }
});
```

Parsers are include for RAML, OpenAPI and the JSON-LD serialisation of the AMF model.

Parsers can accept options, including a hash-map of URLs to local directories that will be used to resolve references in the parsed documents.

For instance, in the next snippet all remote references to the URLs prefixed by `http://test.com/worldmusic` will be resolved looking into the local directory.

```typescript
let apiFile = "http://test.com/something/api.raml";
let cacheDirs = {'cacheDirs': {"http://test.com/something":"file:///path/to/other-examples/world-music-api/api.raml"}};
AMF.RAMLParser.parseFile(apiFile, cacheDirs, (err, model) => {
    if (err != null) {
      console.log(`Created model from file ${model.location()}`);
    }
});
```
The original parsed text can be retrieved using the `rawText` method.


## Navigating the Document Model
The parsing process will return an instance of one of the subclasses of `DocumentModel`.
Depending on what is the parsed file, a `Document`, a `Fragment` or a `Module` instance will be returned.

No matter what is the actual Document Model class, the returned model will also include references to all linked documents in the model.

These references can be listed using the `references` method, and new instances of `DocumentModel` can be built for these references using the `modelForReference` method:

```typescript
let referenceModels = model
  .references()
  .map(ref => model.modelForReference(ref));
```
## Applying resolution

To run the resolution algorithm and combine all the documents from the Document Model into a single Domain Model description, the method `resolve` can be invoked.

```typescript
const resolvedModel: DocumentModel = model.resolve();
```

## Accessing the Domain Model

The parsed Domain Model can be retrieved from the Document Model instance using the appropriate accessor.

Fragments return the encoded Domain Model element using the `encodes` method from the `document.EncodesDomainModel` interface.
Modules returns the list of declared Domain Model elements using the `declares` method from the `document.DeclaresDomainModel` interface.
Documents can use both methods to retrieve the top level encoded element and the list of declared elements in the root element.

```typesocript
if (model instanceof EncodesDomainModel) {
  console.log(model.encodes());
}

if (targetModel instanceof DeclaresDomainModel) {
  model.declares().foreach(decl => console.log(decl);
}
```
## Navigating and mutating the Domain Model

The Domain Model includes matching classes for all elements in the AMF Domain Model.
These getters and setters can be used to navigate and mutate the model. Please, refer to the [documentation](https://raml-org.github.io/api-modeling-framework/doc/js/apidocs/index.html) for more details.

```typescript
let endpoints = api.getEndPoints();

// updating endponts
const before = endpoints.length;
let newEndpoint = EndPoint.build("http://test.com/external/1");
newEndpoint.setPath("/lala");

const afterBreakPoints = endpoints.concat([newEndpoint]);
api.setEndPoints(afterBreakPoints);

// updated endpoints
endpoints = api.getEndPoints();
const after = endpoints.length;

console.log("Should be true " + (after == before + 1));
```
## Serialisation

AMF includes generators capable of serialising the AMF model back into one of the supported syntaxes. The method `generateString` can be used to generate a String representation, and the method `generateFile` can be used to dump the serialised model directly into a file.
Factory methods for each generator can be found in the `AMF` class.


```typescript
AMF.RAMLGenerator.generateString(model, null, null, (e, r) => console.log(r != null));
AMF.OpenAPIGenerator.generateString(model, null, null, (e, r) => console.log(r != null));
AMF.JSONLDGenerator.generateString(model, null, {'full-graph?': true, 'source-maps?': true}, (e, r) => r != null);
```

Two options are available when generating JSON-LD documents.
- `full-graph?` will nest the JSON-LD graphs for the referenced documents in the model to be serialised, otherwise only URIs will be generated.
- `source-maps?` enables or disables the generation of source maps JSON-LD information in the output.
