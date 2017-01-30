(ns raml-framework.model.vocabulary)

(defn document-ns
  ([] "http://raml.org/vocabularies/document#")
  ([s] (str (document-ns) s)))

(defn http-ns
  ([] "http://raml.org/vocabularies/http#")
  ([s] (str (http-ns) s)))

(defn sorg-ns
  ([] "http://schema.org/")
  ([s] (str (sorg-ns) s)))


(def document:Document (document-ns "Document"))
(def document:Fragment (document-ns "Fragment"))
(def document:Module (document-ns "Module"))
(def document:Unit (document-ns "Unit"))
(def document:SourceMap (document-ns "SourceMap"))
(def document:Tag (document-ns "Tag"))
(def document:DomainElement (document-ns "DomainElement"))

(def document:source (document-ns "source"))
(def document:location (document-ns "location"))
(def document:declares (document-ns "declares"))
(def document:encodes (document-ns "encodes"))
(def document:tag (document-ns "tag"))
(def document:tag-id (document-ns "tagId"))
(def document:tag-value (document-ns "tagValue"))



(def http:APIDocumentation (http-ns "APIDocumentation"))

(def sorg:name (sorg-ns "name"))
(def sorg:description (sorg-ns "description"))
(def http:host (http-ns "host"))
(def http:scheme (http-ns "scheme"))
(def http:base-path (http-ns "basePath"))
(def http:accepts (http-ns "accepts"))
(def http:content-type (http-ns "contentType"))
(def sorg:provider (sorg-ns "provider"))
(def http:terms-of-service (http-ns "termsOfService"))
(def sorg:version (sorg-ns "version"))
(def sorg:license (sorg-ns "license"))
