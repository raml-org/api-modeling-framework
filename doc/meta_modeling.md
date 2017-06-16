# Domain meta-modelling using RAML Vocabularies

This document shows a proposed syntax for RAML Vocabularies, a proposed meta-modelling feature for AMF.

The main goals of RAML Vocabularies can be summarised as follows:

- Provide a RAML-like, easy to use syntax to extend RAML domain model
- Provide 'open world' semantics for that syntax thanks to a direct translation into OWL
- Provide 'closed world' semantics for that syntax thanks to a direct translation into SHACL
- Define an inter-operability mechanism with RAML 1.0 that can be used to provide semantic annotations to RAML documents based on the define vocabulary
- Provide a 'syntax' feature that makes it possible to generate a RAML parser for the vocabulary, defining effectively a new RAML dialect


## Vocabulary Namespace

All elements defined in a RAML Vocabulary are identified by single URIs.
In order to compute these URIs a prefix is used in combination with the lexical identifier of the element in the vocabulary declaration to obtain the final URI.
Prefixes for the vocabulary can be introduced using the `base` keyword. If not `base` keyword is present, the URL of the vocabulary file will be used as the prefix.

File: mydomain.raml

Syntax:

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab#"

classTerms:
  Entity:
```

Semantics (open world)

``` turtle
<http://mycorp.com/vocab#Entity> a owl:Class .
```

Semantics (closed world)

``` turtle
```

Without the `base` keyword:

File: mydomain.raml

Syntax:

``` yaml
#% RAML 1.0 Vocabulary

classTerms:
  Entity:
```

Semantics (open world)

``` turtle
<mydomain.raml#Entity> a owl:Class .
```

Semantics (closed world)

``` turtle
```

## Vocabulary Properties

Properties define relations between subject resources and object resources.
Properties are declared as a map of property identifiers to property definition maps, using the `propertyTerms` keyword.


### Simple property

Property relations can have a `range` limiting the valid concepts that can be related with the property. The `range` can have as the value another `Class` or a `DataType`.
In the first case the property is a relation between individuals to individuals in the `range` classes and is called an `ObjectProperty`, if the `range` is a `DataType` the property is a relation between individuals and literal values of the defined `DataType`. These properties are called `DatatypeProperty`.

File: mydomain.raml

``` yaml
#% RAML 1.0 Vocabulary

propertyTerms:
  prop:
    displayName: TheProperty
    description: a property
    range: string
```
Semantics (open world)

``` turtle
<mydomain.raml#prop> a owl:DatatypeProperty ;
  rdfs:label "The Property" ;
  rdfs:comment "a property" ;
  rdfs:range xsd:string .
```

Semantics (closed world)

``` turtle
```

Multiple domains can be specified without problem, the domain in this case is the union of the individuals of both classes.

File: mydomain1.raml

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab1#"

propertyTerms:
  prop:
    displayName: TheProperty
    description: a property
    domain: Class1
    range: string
```

File: mydomain2.raml

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab2#"

uses:
  vocab1: "http://mycorp.com/vocab1#"

propertyTerms:
  vocab1.prop:
    domain: Class2
```

Semantics (open world)

``` turtle
<mydomain.raml/vocab1#p> a owl:DatatypeProperty ;
  rdfs:label "The Property" ;
  rdfs:comment "a property" ;
  rdfs:domain <mydomain.raml/vocab1#Class1>,
              <mydomain.raml/vocab2#Class2> ;
  rdfs:range xsd:string .
```

Semantics (closed world)

``` turtle
```


### Property inheritance

Properties can also form hierarchies of properties with subproperties. If a property P is a subproperty of property P', then all pairs of resources which are related by P are also related by P'.
Subproperties can be expressed using the `extends` keyword.

``` yaml
#% RAML 1.0 Vocabulary

propertyTerms:
  prop:
    displayName: The Property
    description: a property
    range: string

  subprop:
    extends: prop
```

Semantics (open world)

``` turtle
<mydomain.raml#prop> a owl:DatatypeProperty ;
  rdfs:label "The Property" ;
  rdfs:comment "a property" ;
  rdfs:range xsd:string .

<mydomain.raml#subprop> a owl:DatatypeProperty ;
  rdfs:subPropertyOf <mydomain.raml#prop> ;
```

Semantics (closed world)

``` turtle
```

## Vocabulary Classes

Classes can be understood as set of individuals. They are defined using the `classTerm` keyword introducing a mapping from identifiers to class declaration maps.
`displayName` can be used to provide a human readable string for the class and `description` a longer human readable comment about the nature of the class.

### Simple class

File: mydomain.raml

Syntax:

``` yaml
#% RAML 1.0 Vocabulary

classTerms:
  Entity:
    displayName: The Entity
    description: the description.
```


Semantics (open world)

``` turtle
<mydomain.raml#Entity> a owl:Class ;
  rdfs:label "The Entity" ;
  rdfs:comment "the description" .
```

Semantics (closed world)

``` turtle
```


### Class Inheritance

Classes can hold an inheritance relationship. The `extends` keyword can be used to assert that individuals of the described class are also part of the extension of the extended classes.
One `Class` can extend multiple classes.

File: mydomain.raml

Syntax:

``` yaml
#% RAML 1.0 Vocabulary

classTerms:
  Entity:
    displayName: The Entity
    description: the description.
  SubEntity:
    extends: Entity
```


Semantics (open world)

``` turtle
<mydomain.raml#Entity> a owl:Class ;
  rdfs:label "The Entity" ;
  rdfs:comment "the description" .

<mydomain.raml#SubEntity> a owl:Class ;
  rdfs:subClassOf <mydomain.raml#Entity> .
```

Semantics (closed world)

```turtle
```

### Class and properties

Properties can also be provided a `domain`. This is accomplished adding the property to the list of properties defined in a `Class` map declaration, using the `properties` keyword.
If the property is added to more than one list of `properties` the domain of the property is the union of the individuals of all the classes.

File: mydomain.raml

Syntax:

``` yaml
#% RAML 1.0 Vocabulary

propertyTerms:

  Prop1:
    range: string

  Prop2:
    range: OtherEntity

classTerms:

  OtherEntity:

  Entity:
    displayName: The Entity
    description: the description
    properties: [ Prop1, Prop2 ]
```

Semantics (open world)

``` turtle
<mydomain.raml#Prop1> a owl:DatatypeProperty ;
  rdfs:range xsd:string .

<mydomain.raml#Prop2> a owl:ObjectProperty ;
  rdfs:range <mydomain.raml#OtherEntity> .

<mydomain.raml#OtherEntity> a owl:Class .

<mydomain.raml#Entity> a owl:Class ;
  rdfs:label "The Entity" ;
  rdfs:comment "the description" .

<mydomain.raml#Prop1> rdfs:range xsd:string .

<mydomain.raml#Prop2> rdfs:range <mydomain.raml#OtherEntity> .
```
Semantics (closed world)

``` turtle
```

Adding classes to the domain of a property does not impose a restriction over the individuals of the class extension, it is only used to add an assertion indicating that if an individual has that property then it is an instance of all the classes in the domain.

Classes can still be associated to the `syntax` of a class without adding the class of the domain of the property.


## Types annotated with vocabularies


Vocabularies can be used to provide refine the semantics of elements in a RAML 1.0 description like RAML types using a library of 'semantic annotations'.
Through this library, we can use the `(term)` annotation to provide a link between RAML type property or type to a vocabulary `Property` or `Class`.
These annotation can be used to relate data types describing shapes in different data shapes to the same semantic class from a vocabulary.

File: myvocab.raml

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab#"

propertyTerms:
  Prop1:
    range: string
  Prop2:
    range: integer

classTerms:
  Class1:
    properties: [ Prop1, Prop2 ]
```

File: types.raml

``` yaml
#% RAML 1.0

uses:
  # We are going to need for these annotations connecting vocabulary to spec
  vocabulary: "http://raml.org/vocabulary"

(vocabulary.uses):
  v: myvocab.raml

types:
  NewClass1Data:
    (vocabulary.term): v.Class1
    properties:
      prop1:
        (vocabulary.term): v.Prop1
        type: string
      prop2:
        (vocabulary.term): v.Prop2
        type: integer
  Class1Data:
    (vocabulary.term): v.Class1
    properties:
      property_1:
        (vocabulary.term): v.Prop1
```

Semantics (open world, all files)

``` turtle
<http://mycorp.com/vocab#Prop2> a owl:DatatypeProperty ;
  rdfs:domain <http://mycorp.com/vocab#Entity> ;
  rdfs:range xsd:integer .

<http://mycorp.com/vocab#prop1> a owl:DatatypeProperty ;
  rdfs:domain <http://mycorp.com/vocab#Entity> ;
  rdfs:range xsd:string .

<http://mycorp.com/vocab#Class1> a owl:Class .
```

Semantics (closed world, all files)

``` turtle
<types.raml#NewClass1Data/prop1> a sh:PropertyShape ;
  sh:path <mydomain.raml#Prop1> ;
  shapes:propertyLabel "prop1" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:string .

<types.raml#NewClass1Data/prop2> a sh:PropertyShape ;
  sh:path <mydomain.raml#Prop2> ;
  shapes:propertyLabel "prop2" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:integer .

<types.raml#NewClass1Data> a sh:NodeShape ;
  sh:property <types.raml#NewClass1Data/prop1>, <types.raml#NewClass1Data/prop2> .

<types.raml#Class1Data/property_1> a sh:PropertyShape ;
  sh:path <mydomain.raml#Prop1> ;
  shapes:propertyLabel "property_1" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:string .

<types.raml#Class1Data> a sh:NodeShape ;
  sh:property <types.raml#NewClass1Data/property_1> .
```


## Types annotated with vocabularies -> explicit mapping

Annotating all the properties and classes of a RAML specification can be a cumbersome task.
The semantic annotations library provides a `(mapping)` annotation that can be used to build a mapping of syntax terms to `Propeties` and `Classes` that will be applied automatically to all the terms in RAML specification.

File: myvocab.raml

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab#"

propertyTerms:
  Prop1:
    range: string
  Prop2:
    range: integer

classTerms:
  Class1:
    properties: [ Prop1, Prop2 ]
```

File: types.raml

``` yaml
#% RAML 1.0

uses:
  # We are going to need for these annotations connecting vocabulary to spec
  vocabulary: "http://raml.org/vocabulary"

(vocabulary.uses):
  v: myvocab.raml

# This mapping is applied to all raml types in the API
# Useful when labels appear many times in the specification
(vocabulary.mapping):
  explicit:
    NewClass1Data: v.Class1
    Class1Data: v.Class1
    prop1: v.Prop1
    prop2: v.Prop2

types:
  NewClass1Data:
    properties:
      prop1:
        type: string
      prop2:
        type: integer
  Class1Data:
    properties:
      property_1:
        # we can still overwrite
        (vocabulary.term): v.Prop1
```

## Types annotated with vocabularies -> implicit mapping

In some occasions, creating the mapping is a very repetitive task because the name of the `Properties` or `Classes` match the syntax terms used in the RAML spec.
In this situations, the `implicit` value for the `(mapping)` annotation can be used. If this is enabled, the parser will try to annotate automatically any syntax property matching the identifier (minus the prefix) in the loaded vocabularies.
A simple heuristic based on ignoring case and removing dashes and underscores will be used for the matching.

This feature can be combined with the explicit mapping of terms.

If more than one `Property` or `Class` is found matching a term, an exception will be thrown by the parser an explicit annotations should be used.

File: myvocab.raml

``` yaml
#% RAML 1.0 Vocabulary

base: "http://mycorp.com/vocab#"

propertyTerms:
  Prop1:
    range: string
  Prop2:
    range: integer

classTerms:
  Class1:
    properties: [ Prop1, Prop2 ]
```

File: types.raml

``` yaml
#% RAML 1.0

uses:
  # We are going to need for these annotations connecting vocabulary to spec
  vocabulary: "http://raml.org/vocabulary"

(vocabulary.uses):
  v: myvocab.raml

# This mapping tries to find matching terms from the
# loaded vocabularies in (domain.vocabularies)
# using simple heuristics
# (toLowerCase (join (split /-_/ term) ""))
# It should take care of collisions between terms from
# different vocabularies and throw an error if they exist
(vocabulary.mapping):
  # false by default
  implicit: true
  # it can be combined with the explicit one
  explicit:
    property_1: v.Prop1

types:
  NewClass1Data:
    (vocabulary.term): v.Class1
    properties:
      prop1:
        type: string
      prop2:
        type: integer
  Class1Data:
    (vocabulary.term): v.Class1
    properties:
      property_1:
```

## Types annotated with 'external' vocabulary

The semantic annotations library also allows to use 'external' vocabularies through the `(external)` annotation.
External vocabularies are defined by a prefix that will be used to compute the URIs for the semantic annotations.
The prefix will not validated or processed in anyway other than computing the URIs for the terms.

This mechanism can be used, for example, to annotate RAML documents with vocabularies like Schema.org.


``` yaml
#% RAML 1.0

uses:
  # We are going to need for these annotations connecting vocabulary to spec
  vocabulary: "http://raml.org/vocabulary"

# The URL is not processed, just used
# by the parser to build the resulting URIs for the terms
# (The parser does not need to understand the external vocabulary, and it can be in any format)
(vocabulary.external):
  sorg: "http://schema.org/"

(vocabulary.mapping):
  # it can be combined with the explicit one
  explicit:
    NewClass1Data: sorg.Person
    Class1Data: sorg.Organization
    prop1: sorg.name
    property_1: sorg.name
    prop2: sorg.age

types:
  NewClass1Data:
    properties:
      prop1:
        type: string
      prop2:
        type: integer
  Class1Data:
    properties:
      property_1:
```


Semantics (closed world, all files)

``` turtle
<types.raml#NewClass1Data/prop1> a sh:PropertyShape ;
  sh:path sorg:name ;
  shapes:propertyLabel "prop1" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:string .

<types.raml#NewClass1Data/prop2> a sh:PropertyShape ;
  sh:path sorg:age ;
  shapes:propertyLabel "prop2" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:integer .

<types.raml#NewClass1Data> a sh:NodeShape ;
  sh:property <types.raml#NewClass1Data/prop1>, <types.raml#NewClass1Data/prop2> .

<types.raml#Class1Data/property_1> a sh:PropertyShape ;
  sh:path sorg:name ;
  shapes:propertyLabel "property_1" ;
  sh:minCount 1 ;
  sh:maxCount 1 ;
  sh:datatype xsd:string .

<types.raml#Class1Data> a sh:NodeShape ;
  sh:property <types.raml#NewClass1Data/property_1> .
```
