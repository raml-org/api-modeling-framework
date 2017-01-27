(ns raml-framework.model.namespaces)

(defn model-ns
  ([] "http://raml.org/vocabularies/model#")
  ([s] (str (model-ns) s)))
