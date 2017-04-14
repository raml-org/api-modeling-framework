(ns api-modeling-framework.js-support
  (:require [cljs.nodejs :as node]
            [taoensso.timbre :as timbre :refer [debug]]))

;; setting node's fs module
;; we cannot use it directly in the code because it interfers
(def fs (node/require "fs"))
(aset js/global "NODE_FS" fs)

;; Let's load node platform dependencies from the local JS platform file.
;; These will be introduced in the build in the web version using a foreign-lib

;; Both versions communicate by declaring global variables: JS_YAML and JS_REST
(try
  (debug "Loading Node JS dependencies from:" (str js/__dirname "/js/yaml.js"))
  (node/require (str js/__dirname "/js/yaml.js"))
  (catch js/Error e
    (debug "Not found, test build? Trying:" (str js/__dirname "/../../../js/yaml.js"))
    (node/require (str js/__dirname "/../../../js/yaml.js"))))
