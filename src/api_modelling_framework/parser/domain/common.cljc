(ns api-modelling-framework.parser.domain.common
  (:require [api-modelling-framework.model.document :as document]
            [api-modelling-framework.utils :as utils]))

(defn generate-is-type-sources [type-name location parsed-location]
  (let [source-map-id (utils/path-join parsed-location "/source-map/is-type")
        is-type-tag (document/->IsTypeTag source-map-id type-name)]
    [(document/->DocumentSourceMap (utils/path-join parsed-location "/source-map") location [is-type-tag])]))
