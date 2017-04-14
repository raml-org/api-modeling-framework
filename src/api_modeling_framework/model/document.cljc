(ns api-modeling-framework.model.document)

(defprotocol Node
  "Basic behaviour for every single node in the model"
  (id [this] "Returns a unique identifier for this node")
  (name [this] "A string readable title for this node")
  (description [this] "A human readable description of this node")
  (sources [this] "Collection of source maps for this node")
  (valid? [this] "Checks if this node is valid")
  (extends [this] "Nodes extended by this node")
  (additional-properties [this] "List of additional properties associated to this node"))

(defprotocol Extends
  (arguments [this] "Arguments for the extension"))

(defprotocol Includes
  (target [this] "Target of the relationship")
  (label  [this] "Description of the kind of relationship"))

(defn includes-element? [x] (satisfies? Includes x))

(defrecord ParsedExtends [id name description sources additional-properties target label arguments]
  Includes
  (target [this] target)
  (label  [this] label)
  Extends
  (arguments [this] (or arguments []))
  Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (extends [this] [])
  (additional-properties [this] []))

(defrecord ParsedIncludes [id name description sources additional-properties target label ]
  Includes
  (target [this] target)
  (label  [this] label)
  Node
  (id [this] id)
  (name [this] name)
  (description [this] description)
  (sources [this] sources)
  (extends [this] [])
  (additional-properties [this] []))

(defprotocol SourceMap
  "Defines basic behaviour for all nodes in the model"
  (source [this] "URI identifying the location of the information source in the layer n - 1
                    In the document model, the location will be a file or HTTP resource, in the service layer, it will be a node in the document layer.")
  (tags [this] "Annotations describing the kind of relationship between the source of information and the target node"))

(defrecord DocumentSourceMap [id source tags additional-properties]
  SourceMap
  (source [this] source)
  (tags [this] tags)
  Node
  (id [this] id)
  (name [this] "Document source map")
  (description [this] (str "Source map for a document located at " source))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] (or additional-properties [])))

(defprotocol Tag
  (tag-id [this] "Identifier for the tag")
  (value [this] "A value associated to this tag"))

(defn find-tag [node tag-id-to-find]
  (let [sources (or (:sources  node) (try (sources node) (catch #?(:cljs js/Error :clj Exception) ex nil)))]
    (if (some? sources)
      (->> sources
           (map tags)
           flatten
           (filter (fn [tag] (= tag-id-to-find (tag-id tag))))
           flatten
           (filter some?))
      [])))

(defn replace-tag [node old-tag new-tag]
  (let [sources (or (:sources  node) (try (sources node) (catch #?(:cljs js/Error :clj Exception) ex nil)))]
    (if (some? sources)
      (let [new-sources (loop [old-sources sources
                               new-sources []]
                          (if (empty? old-sources)
                            new-sources
                            (let [next-source (first old-sources)
                                  old-tag-id (tag-id old-tag)
                                  new-source (assoc next-source :tags (->> (tags next-source)
                                                                           (mapv (fn [tag] (if (= old-tag-id (tag-id tag))
                                                                                            new-tag
                                                                                            tag)))))]
                              (recur (rest old-sources)
                                     (conj new-sources new-source)))))]
        (assoc node :sources new-sources))
      node)))

(defn remove-tag [node tag-to-remove]
  (let [sources (or (:sources  node) (try (sources node) (catch #?(:cljs js/Error :clj Exception) ex nil)))]
    (if (some? sources)
      (let [new-sources (loop [old-sources sources
                               new-sources []]
                          (if (empty? old-sources)
                            new-sources
                            (let [next-source (first old-sources)
                                  new-source (assoc next-source :tags (->> (tags next-source)
                                                                           (filter (fn [tag]
                                                                                     (not= tag-to-remove (tag-id tag))))))
                                  new-sources (if (> (count (:tags new-source)) 0)
                                                (conj new-sources new-source)
                                                new-sources)]
                              (recur (rest old-sources)
                                     new-sources))))]
        (assoc node :sources new-sources))
      node)))

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
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

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
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(def node-parsed-tag "node-parsed")

(defrecord NodeParsedTag [id path]
  Tag
  (tag-id [this] node-parsed-tag)
  (value [this] path)
  Node
  (id [this] id)
  (name [this] "Node parsed tag")
  (description [this] (str "This node was generating parsing a node located at " path))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(def nested-resource-path-parsed-tag "nested-resource-path-parsed")

(defrecord NestedResourcePathParsedTag [id path]
  Tag
  (tag-id [this] nested-resource-path-parsed-tag)
  (value [this] path)
  Node
  (id [this] id)
  (name [this] "Nested resource parsed tag")
  (description [this] (str "This node was generating parsing a nested resource with path " path))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(def nested-resource-children-tag "nested-resource-nested-children")

(defrecord NestedResourceChildrenTag [id children-id]
  Tag
  (tag-id [this] nested-resource-children-tag)
  (value [this] children-id)
  Node
  (id [this] id)
  (name [this] "Resource nested children tag")
  (description [this] (str "This resource has a nested resource " children-id))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(def nested-resource-parent-id-tag "nested-resource-parent-id")

(defrecord NestedResourceParentIdTag [id parent-id]
  Tag
  (tag-id [this] nested-resource-parent-id-tag)
  (value [this] parent-id)
  Node
  (id [this] id)
  (name [this] "Resource parent id  tag")
  (description [this] (str "This resource has a parent resource " parent-id))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(def api-tag-tag "api-tag-tag")

(defrecord APITagTag [id tag-value]
  Tag
  (tag-id [this] api-tag-tag)
  (value [this] tag-value)
  Node
  (id [this] id)
  (name [this] "API specific tag")
  (description [this] (str "API specific tag with value " tag-value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; used to makr that some declared extended fragments in a document are
;; related to this node, for example as traits or types
(def inline-fragment-parsed-tag "inline-fragment-parsed-tag")

(defrecord InlineFragmentParsedTag [id tag-value]
  Tag
  (tag-id [this] inline-fragment-parsed-tag)
  (value [this] tag-value)
  Node
  (id [this] id)
  (name [this] "Inline fragment  tag")
  (description [this] (str "Inline fragment tag with value " tag-value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; used to mark that an included fragment has been extended by the node
  (def extend-include-fragment-parsed-tag "extend-include-fragment-parsed-tag")

(defrecord ExtendIncludeFragmentParsedTag [id tag-value]
  Tag
  (tag-id [this] extend-include-fragment-parsed-tag)
  (value [this] tag-value)
  Node
  (id [this] id)
  (name [this] "Extend Include Fragment")
  (description [this] (str "Extend Include Fragment tag with value " tag-value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; Is trait is used to mark that the fragment is a RAML trait
(def is-trait-tag "is-trait-tag")

(defrecord IsTraitTag [id value]
  Tag
  (tag-id [this] is-trait-tag)
  (value [this] value)
  Node
  (id [this] id)
  (name [this] "Is trait tag")
  (description [this] (str "Is trait tag with name " value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; Is type is used to mark that the fragment is a RAML type
(def is-type-tag "is-type-tag")

(defrecord IsTypeTag [id value]
  Tag
  (tag-id [this] is-type-tag)
  (value [this] value)
  Node
  (id [this] id)
  (name [this] "Is type tag")
  (description [this] (str "Is type tag with name " value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; Is annotation is used to mark that the fragment is a RAML annotation
(def is-annotation-tag "is-annotation-tag")

(defrecord IsAnnotationTag [id value]
  Tag
  (tag-id [this] is-annotation-tag)
  (value [this] value)
  Node
  (id [this] id)
  (name [this] "Is annotation tag")
  (description [this] (str "Is annotation tag with name " value))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; Extends trait is used to mark that an extend relationship in a node is actually the application of a RAML trait
(def extends-trait-tag "extends-trait-tag")

(defrecord ExtendsTraitTag [id trait-name]
  Tag
  (tag-id [this] extends-trait-tag)
  (value [this] trait-name)
  Node
  (id [this] id)
  (name [this] "Extends trait tag")
  (description [this] (str "Extends trait tag with name " trait-name))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

;; Uses library is used to mark that a particular module is a RAML library/document using another library
(def uses-library-tag "uses-library-tag")

(defrecord UsesLibraryTag [id library-alias library]
  Tag
  (tag-id [this] uses-library-tag)
  (value [this] library)
  Node
  (id [this] id)
  (name [this] library-alias)
  (description [this] (str "Uses library tag with name " library-alias " located at " library))
  (sources [this] [])
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] []))

(defprotocol Fragment
  "Units encoding domain fragments"
  (encodes [this] "Domain description encoded into this document unit"))

(defprotocol Module
  "Units containing abstract fragments that can be referenced from other fragments"
  (declares [this] "Fragments that are exposed for other units to refer"))

(defprotocol Unit
  "Any parseable unit, it should be backed by a source URI"
  (resolved [this] "Has this model being resolved")
  (location [this] "Derefenceable location of the unit")
  (references [this] "Fragments referenced by this document"))

(defn generate-document-sources [source document-type]
  (let [source-map-id (str source "#/source-map/0")
        file-parsed-tag (if (some? source)
                          (FileParsedTag. (str source-map-id "/tag/file-parsed") source)
                          nil)
        document-type-tag (if (some? document-type)
                            (DocumentTypeTag. (str source-map-id "/tag/document-type") document-type)
                            nil)]
    (if (some? source)
      [(DocumentSourceMap. source-map-id source (filter some? [file-parsed-tag document-type-tag]) [])]
      [])))

(defrecord ParsedDocument [location encodes declares references additional-properties document-type resolved raw]
  Node
  (id [this] location)
  (name [this] location)
  (description [this] nil)
  (sources [this] (generate-document-sources location document-type))
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] (or additional-properties []))
  Unit
  (location [this] location)
  (references [this] (or references []))
  (resolved [this] (or resolved false))
  Fragment
  (encodes [this] encodes)
  Module
  (declares [this] (or declares [])))

(defrecord ParsedModule [location declares references additional-properties document-type resolved description raw]
  Node
  (id [this] location)
  (name [this] location)
  (description [this] description)
  (sources [this] (generate-document-sources location document-type))
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] (or additional-properties []))
  Unit
  (location [this] location)
  (references [this] (or references []))
  (resolved [this] (or resolved false))
  Module
  (declares [this] (or declares [])))

(defrecord ParsedFragment [location encodes additional-properties references document-type resolved raw]
  Node
  (id [this] location)
  (name [this] location)
  (description [this] nil)
  (sources [this] (generate-document-sources location document-type))
  (valid? [this] true)
  (extends [this] [])
  (additional-properties [this] (or additional-properties []))
  Unit
  (location [this] location)
  (references [this] (or references []))
  (resolved [this] (or resolved false))
  Fragment
  (encodes [this] encodes))
