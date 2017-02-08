(ns api-modelling-framework.generators.domain.utils
  (:require [api-modelling-framework.model.document :as document]
            [api-modelling-framework.model.domain :as domain]))

(defn send
  "Dispatchs a method to an object, if the object is an inclusion relationship, it searches in the included object
   in the context, builds the domain object and sends the method"
  [method obj {:keys [fragments]}]
  (if (document/includes-element? obj)
    (let [fragment (get fragments (document/target obj))]
      (if (nil? fragment)
        (throw (new #?(:clj Exception :cljs js/Error)
                    (str "Cannot find fragment " (document/target obj) " to apply method " method)))
        (apply method [(-> fragment document/encodes domain/to-domain-node)])))
    (apply method [obj])))

(defn <-domain [obj {:keys [fragments]}]
  (if (document/includes-element? obj)
    (let [fragment (get fragments (document/target obj))]
      (if (nil? fragment)
        (throw (new #?(:clj Exception :cljs js/Error)
                    (str "Cannot find fragment " (document/target obj))))
        (-> fragment document/encodes domain/to-domain-node)))
    obj))
