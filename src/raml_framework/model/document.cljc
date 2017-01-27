(ns raml-framework.model.document)

(defprotocol Node
  "Basic behaviour for every single node in the model"
  (id [this] "Returns a unique identifier for this node")
  (name [this] "A string readable title for this node")
  (description [this] "A human readable description of this node")
  (sources [this] "Collection of source maps for this node")
  (to-jsonld [this source-maps?] "Returns a JSON-LD representation for this node")
  (valid? [this] "Checks if this node is valid"))

(defprotocol SourceMap
  "Defines basic behaviour for all nodes in the model"
  (location [this] "URI identifying the location of the information source in the layer n - 1
                    In the document model, the location will be a file or HTTP resource, in the service layer, it will be a node in the document layer.")
  (tags [this] "Annotations describing the kind of relationship between the source of information and the target node"))

(defrecord DocumentSourceMap [id location tags]
  SourceMap
  (location [this] location)
  (tags [this] tags)
  Node
  (id [this] id)
  (name [this] "Document source map")
  (description [this] (str "Source map for a document located at " location))
  (sources [this] [])
  (to-jsonld [this source-maps?] nil)
  (valid? [this] true))

(defprotocol Tag
  (tag-id [this] "Identifier for the tag")
  (value [this] "A value associated to this tag"))

(def file-parsed-tag "file-parsed")

(defrecord FileParsedTag [id location]
  Tag
  (tag-id [this] file-parsed-tag)
  (value [this] location)
  Node
  (id [this] id)
  (name [this] "File parsed tag")
  (description [this] (str "This node was generating parsing a file located at " location))
  (sources [this] [])
  (to-jsonld [this source-maps?] nil)
  (valid? [this] true))

(def document-type-tag "document-type")

(defrecord DocumentTypeTag [id document-type]
  Tag
  (tag-id [this] document-type-tag)
  (value [this] document-type)
  Node
  (id [this] id)
  (name [this] "Document type tag")
  (description [this] (str "This node was generating parsing a file of type " document-type))
  (sources [this] [])
  (to-jsonld [this source-maps?] nil)
  (valid? [this] true))


(defprotocol DocumentUnit
  "Any parseable unit, it should be backed by a source URI"
  (encodes [this] "Domain description encoded into this document unit")
  (declares [this] "Depdencies of this unit with other parsing units"))



(defn generate-document-sources [location document-type]
  (let [source-map-id (str location "#/source-map/0")
        file-parsed-tag (if (some? location)
                          (FileParsedTag. (str source-map-id "/tag/file-parsed") location)
                          nil)
        document-type-tag (if (some? document-type)
                            (DocumentTypeTag. (str source-map-id "/tag/document-type") document-type)
                            nil)]
    (if (some? location)
      [(DocumentSourceMap. source-map-id location (filter some? [file-parsed-tag document-type-tag]))]
      [])))

(defrecord Document [location encodes declares document-type]
  Node
  (id [this] location)
  (name [this] location)
  (description [this] (str "Document parsed from " location " encoding " document-type " information "))
  (sources [this] (generate-document-sources location document-type))
  (to-jsonld [this source-maps?] nil)
  (valid? [this] true)
  DocumentUnit
  (encodes [this] encodes)
  (declares [this] declares))
