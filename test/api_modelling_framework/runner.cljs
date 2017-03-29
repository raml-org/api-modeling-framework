(ns api-modelling-framework.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [api-modelling-framework.tck]
            [api-modelling-framework.integration-test]
            [api-modelling-framework.utils-test]
            [api-modelling-framework.parser.syntax.yaml-test]
            [api-modelling-framework.parser.syntax.json-test]
            [api-modelling-framework.parser.document.jsonld-test]
            [api-modelling-framework.parser.domain.jsonld-test]
            [api-modelling-framework.parser.domain.raml-test]
            [api-modelling-framework.parser.domain.openapi-test]
            [api-modelling-framework.generators.document.jsonld-test]
            [api-modelling-framework.generators.document.raml-test]
            [api-modelling-framework.generators.domain.jsonld-test]
            [api-modelling-framework.generators.domain.openapi-test]
            [api-modelling-framework.generators.domain.raml-test]
            [api-modelling-framework.generators.document.openapi-test]
            [api-modelling-framework.model.document-test]
            ))

(doo-tests 'api-modelling-framework.integration-test
           'api-modelling-framework.utils-test
           'api-modelling-framework.parser.syntax.yaml-test
           'api-modelling-framework.parser.syntax.json-test
           'api-modelling-framework.model.document-test
           'api-modelling-framework.generators.document.jsonld-test
           'api-modelling-framework.parser.document.jsonld-test
           'api-modelling-framework.parser.domain.jsonld-test
           'api-modelling-framework.parser.domain.raml-test
           'api-modelling-framework.parser.domain.openapi-test
           'api-modelling-framework.generators.domain.jsonld-test
           'api-modelling-framework.generators.domain.openapi-test
           'api-modelling-framework.generators.domain.raml-test
           'api-modelling-framework.generators.document.raml-test
           'api-modelling-framework.generators.document.openapi-test
           'api-modelling-framework.tck
           )
