(ns api-modeling-framework.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [api-modeling-framework.tck]
            [api-modeling-framework.integration-test]
            [api-modeling-framework.utils-test]
            [api-modeling-framework.parser.syntax.yaml-test]
            [api-modeling-framework.parser.syntax.json-test]
            [api-modeling-framework.parser.document.jsonld-test]
            [api-modeling-framework.parser.domain.jsonld-test]
            [api-modeling-framework.parser.domain.raml-test]
            [api-modeling-framework.parser.domain.openapi-test]
            [api-modeling-framework.generators.document.jsonld-test]
            [api-modeling-framework.generators.document.raml-test]
            [api-modeling-framework.generators.domain.jsonld-test]
            [api-modeling-framework.generators.domain.openapi-test]
            [api-modeling-framework.generators.domain.raml-test]
            [api-modeling-framework.generators.document.openapi-test]
            [api-modeling-framework.model.document-test]
            [api-modeling-framework.core-test]
            [api-modeling-framework.parser.domain.common-test]
            ))

(doo-tests 'api-modeling-framework.tck
           'api-modeling-framework.integration-test
           'api-modeling-framework.utils-test
           'api-modeling-framework.parser.syntax.yaml-test
           'api-modeling-framework.parser.syntax.json-test
           'api-modeling-framework.model.document-test
           'api-modeling-framework.generators.document.jsonld-test
           'api-modeling-framework.parser.document.jsonld-test
           'api-modeling-framework.parser.domain.jsonld-test
           'api-modeling-framework.parser.domain.raml-test
           'api-modeling-framework.parser.domain.openapi-test
           'api-modeling-framework.generators.domain.jsonld-test
           'api-modeling-framework.generators.domain.openapi-test
           'api-modeling-framework.generators.domain.raml-test
           'api-modeling-framework.generators.document.raml-test
           'api-modeling-framework.generators.document.openapi-test
           'api-modeling-framework.core-test
           'api-modeling-framework.parser.domain.common-test
           )
