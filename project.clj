(defproject raml-framework "0.1.0-SNAPSHOT"
  :description "Parsing tools for API specs"
  :url "https://github.com/mulesoft-labs/raml-framework"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [io.swagger/swagger-parser "1.0.21"]
                 [org.clojure/core.async "0.2.395"]
                 [cheshire "5.6.3"]
                 [com.cemerick/url "0.1.1"]
                 [clj-yaml "0.4.0" :exclusions [[org.yaml/snakeyaml]]]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-npm "0.6.2"]
            [lein-doo "0.1.7"]]
  :npm {:dependencies [[uri-templates "0.2.0"]
                       [sync-request "3.0.1"]
                       [js-yaml "3.7.0"]]}

  :cljsbuild {:builds {:default {:source-paths ["src"]
                                 :figwheel true
                                 :compiler {:main raml-framework.core.js
                                            :output-dir "node/engine"
                                            :output-to "node/engine/index.js"
                                            :optimizations :none,
                                            :source-map true,
                                            :source-map-timestamp true,
                                            :recompile-dependents false,
                                            :pretty-print true
                                            :target :nodejs}}
                       :test    {:source-paths ["src" "test"]
                                 :compiler {:output-to "resources/public/js/main-test.js"
                                            :main raml-framework.runner
                                            :pretty-print true
                                            :target :nodejs}}}})
