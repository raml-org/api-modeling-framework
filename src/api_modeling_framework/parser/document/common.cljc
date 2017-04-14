(ns api-modeling-framework.parser.document.common
  (:require [clojure.set :refer [difference]]))

(defn get-one
  ([x prop default]
   (if (some? (get x prop))
     (let [value (get x prop)]
       (if (coll? value)
         (first value)
         value))
     default))
  ([x prop] (get-one x prop nil)))


(defn make-compute-fragments [fragments]
  (let [initial-fragments (set (vals @fragments))]
    (fn [final-fragments]
      (let [final-fragments (set final-fragments)]
        (->> (difference final-fragments initial-fragments)
             (into []))))))
