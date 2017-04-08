(ns api-modelling-framework.platform.extras
  (:require [cljs.nodejs :as node]))

(def rest (node/require "rest"))

(defn load[location cb]
  (-> (rest location)
      (.then
       (fn [response] (cb nil (.entity response))))
      (.catch
       (fn [e] (cb {:error err} nil)))))
