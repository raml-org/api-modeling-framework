(ns raml-framework.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [raml-framework.parser.syntax.yaml-test]
            ))

(doo-tests 'raml-framework.parser.syntax.yaml-test
           )
