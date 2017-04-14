(ns api-modeling-framework.generators.domain.utils
  (:require [api-modeling-framework.model.document :as document]
            [api-modeling-framework.model.domain :as domain]))

(defn send
  "Dispatchs a method to an object, if the object is an inclusion relationship, it searches in the included object
   in the context, builds the domain object and sends the method"
  [method obj {:keys [fragments]}]
  (if (document/includes-element? obj)
    (let [fragment (get fragments (document/target obj))]
      (if (nil? fragment)
        (throw (new #?(:clj Exception :cljs js/Error)
                    (str "Cannot find fragment " (document/target obj) " to apply method " method)))
        (apply method [(document/encodes fragment)])))
    (apply method [obj])))

(defn <-domain [obj {:keys [fragments references] :as context}]
  (if (document/includes-element? obj)
    (let [fragment-target (document/target obj)
          fragment (get fragments fragment-target)
          reference (->> references
                         (filter #(= (document/id %) fragment-target))
                         first)]
      (cond
        ;; if it is a fragment, is a link to an external node, I need to generate the document
        (some? fragment) (document/encodes fragment)
        ;; If it is a reference, is a link to internally defined node, I just need the reference
        (some? reference)  obj
        ;; Unknown reference @todo Should I throw an exception in this case?
        :else              (throw (new #?(:clj Exception :cljs js/Error)
                                       (str "Cannot find fragment " (document/target obj))))))
    obj))
