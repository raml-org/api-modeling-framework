(ns raml-framework.model.vocabulary)

(defn model-ns
  ([] "http://raml.org/vocabularies/model#")
  ([s] (str (model-ns) s)))



(def model:Document (model-ns "Document"))
(def model:Unit (model-ns "Unit"))
(def model:SourceMap (model-ns "SourceMap"))
(def model:Tag (model-ns "Tag"))

(def model:source (model-ns "source"))
(def model:location (model-ns "location"))
(def model:declares (model-ns "declares"))
(def model:encodes (model-ns "encodes"))
(def model:tag (model-ns "tag"))
(def model:tag-id (model-ns "tagId"))
(def model:tag-value (model-ns "tagValue"))
