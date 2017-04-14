(ns api-modeling-framework.parser.syntax.common)

;; This function is only used by the JS parser in YAML.
;; We transform the additional property into
;; lexical meta-data for the parsed node.
;; The Java version in clj-yaml already generates the
;; lexical meta-data from the AST information

;; Now is also being used by the JSON/AST parser
(defn add-location-meta [node]
  (cond
    (map? node)  (let [location-kw (get node (keyword "__location__"))
                       location-str (get node "__location__")]
                   (if (some? (or location-kw location-str))
                     (with-meta
                       (->> (-> node
                                (dissoc (keyword "__location__"))
                                (dissoc "__location__"))
                            (mapv (fn [[k v]] [k (add-location-meta v)]))
                            (into {}))
                       (or location-str location-kw))
                     (->> (-> node
                              (dissoc (keyword "__location__"))
                              (dissoc "__location__"))
                          (mapv (fn [[k v]] [k (add-location-meta v)]))
                          (into {}))))
    (coll? node) (mapv add-location-meta node)
    :else        node))
