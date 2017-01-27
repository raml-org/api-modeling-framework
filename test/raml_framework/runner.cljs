(ns raml-framework.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [raml-framework.parser.syntax.yaml-test]
            [raml-framework.parser.syntax.json-test]
            [raml-framework.parser.document.jsonld-test]
            [raml-framework.generators.document.jsonld-test]
            [raml-framework.model.document-test]
            ))

(doo-tests 'raml-framework.parser.syntax.yaml-test
           'raml-framework.parser.syntax.json-test
           'raml-framework.model.document-test
           'raml-framework.generators.document.jsonld-test
           'raml-framework.parser.document.jsonld-test
           )
