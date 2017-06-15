(ns api-modeling-framework.utils-test
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [clojure.test :refer [deftest is]]
            #?(:cljs [cljs.core.async :refer [<! >! chan]])
            #?(:clj [clojure.core.async :refer [go <! >! chan]])
            [api-modeling-framework.utils :as utils]))

(deftest safe-str-test
  (is (= (utils/safe-str "test") "test"))
  (is (= (utils/safe-str :test) "test"))
  (is (= (utils/safe-str (keyword "/test")) "/test"))
  (is (= (utils/safe-str (keyword "application/json")) "application/json")))


(def cb->chan utils/cb->chan)

(defn error? [x]
  (if (and (map? x)
           (some? (:error x)))
    (do (prn (:error x))
        true)
    false))


(deftest node->uri->node-test
  (let [nodes ["API" "DocumentationItem" "Resource" "Method" "Response" "RequestBody" "ResponseBody" "TypeDeclaration"
               "Example" "ResourceType" "Trait" "SecurityScheme"
               "SecuritySchemeSettings" "AnnotationType" "Library" "Overlay" "Extension"]]
    (doseq [node nodes]
      (is (= node ((comp utils/domain-uri->node-name utils/node-name->domain-uri) node))))))
