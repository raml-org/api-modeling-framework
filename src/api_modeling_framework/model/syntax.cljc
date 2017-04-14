(ns api-modeling-framework.model.syntax)

(defn fragment? [unit]
  (some? (get unit (keyword "@location"))))


(defn <-data [document]
  (get document (keyword "@data")))

(defn <-location [document]
  (get document (keyword "@location")))

(defn <-fragment [document]
  (get document (keyword "@fragment")))


(def at-data (keyword "@data"))
(def at-location (keyword "@location"))
(def at-fragment (keyword "@fragment"))
