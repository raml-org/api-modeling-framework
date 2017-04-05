(ns api-modelling-framework.generators.syntax.jsonld
  #?(:cljs (:require [api-modelling-framework.platform :as platform]
                     [api-modelling-framework.model.vocabulary :as v]))

  #?(:clj (:require [api-modelling-framework.platform :as platform]
                    [api-modelling-framework.model.vocabulary :as v])))



(defn clean-references
  "We don't serialise all the references for the model, just the id and the types"
  ([document]
   (let [references (->> (get document v/document:references [])
                         (map (fn [ref] {"@id" (get ref "@id")
                                        "@type" (get ref "@type")}))
                         (filter (fn [ref] (some? (get ref "@id")))))]
     (if (> (count references) 0)
       (assoc document v/document:references references)
       (dissoc document v/document:references)))))

(defn generate-string
  ([document full-graph?] (platform/encode-json (if full-graph? document (clean-references document))))
  ([document] (generate-string document true)))

(defn generate-file
  ([location document full-graph?]
   (platform/write-location location (platform/encode-json (if full-graph? (clean-references document)))))
  ([location document]
   (generate-file location document true)))
