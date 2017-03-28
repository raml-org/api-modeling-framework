(ns api-modelling-framework.parser.document.common)

(defn get-one
  ([x prop default]
   (if (some? (get x prop))
     (let [value (get x prop)]
       (if (coll? value)
         (first value)
         value))
     default))
  ([x prop] (get-one x prop nil)))
