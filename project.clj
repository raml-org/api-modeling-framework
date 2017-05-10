(defproject api-modeling-framework "0.1.2-SNAPSHOT"

  :description "API and domain modeling tools for RAML, OpenAPI (Swagger) and RDF"

  :url "https://github.com/mulesoft-labs/api-modeling-framework"

  :license {:name "Apache-2.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.3.442"]
                 [cheshire "5.6.3"]
                 [instaparse "1.4.2"]
                 [com.lucasbradstreet/instaparse-cljs "1.4.1.2"]
                 [com.cemerick/url "0.1.1"]
                 [com.taoensso/timbre "4.8.0"]
                 [org.yaml/snakeyaml "1.12"]
                 ;; dev only
                 [difform "1.1.2"]]

  :aot [api-modeling-framework.model.domain]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-npm "0.6.2"]
            [lein-doo "0.1.7"]]
  :npm {:dependencies [[uri-templates "0.2.0"]
                       [rest "2.0.0"]
                       [json-to-ast "2.0.0-alpha1.2"]]}

  :profiles {:build {:source-paths ["build"]
                     :main api-modeling-framework.build}
             :precomp {:aot [api-modeling-framework.model.domain] }
             :java-compile {:source-paths []
                            :java-source-paths ["java/src"]}}

  :aliases {"node" ["with-profile" "build" "run" "node"]
            "web" ["with-profile" "build" "run" "web"]
            "js-bindings-web" ["with-profile" "build" "run" "js-bindings-web"]
            "js-bindings-node" ["with-profile" "build" "run" "js-bindings-node"]
            "test-js" ["doo" "node" "test" "once"]}

  :cljsbuild {:builds {
                       :node {:source-paths ["src", "src_node"]
                              :figwheel true
                              :compiler {:main api-modeling-framework.core
                                         :output-dir "output/node/"
                                         :output-to "output/node/amf.js"
                                         :optimizations :none,
                                         :source-map true,
                                         :source-map-timestamp true,
                                         :recompile-dependents false,
                                         :pretty-print true
                                         :target :nodejs}}

                       :web     {:source-paths ["src"]
                                 :figwheel true
                                 :compiler {:output-to "output/web/amf.js"
                                            :main api-modeling-framework.core
                                            :asset-path "/js"
                                            ;:optimizations :whitespace
                                            :optimizations :advanced
                                            :foreign-libs [{:file "js/js-support-bundle.js"
                                                            :provides ["api_modeling_framework.js-support"]}]
                                            :externs ["js/externs.js"]
                                            :pretty-print true}}

                       :bindings {:source-paths ["src"]
                                 :figwheel true
                                 :compiler {:main api-modeling-framework.core

                                            :output-dir "output/bindings/"
                                            :output-to "output/bindings/amf.js"
                                            :optimizations :simple,
                                            :target :nodejs

                                            :asset-path "/js"
                                            :foreign-libs [{:file "js/js-support-bundle.js"
                                                            :provides ["api_modeling_framework.js-support"]}]
                                            :externs ["js/externs.js"]
                                            :pretty-print true}}

                       :test    {:source-paths ["src" "src_node" "test"]
                                 :compiler {:output-dir "output/test/"
                                            :output-to "output/test/amf-test.js"
                                            :main api-modeling-framework.runner
                                            :pretty-print true
                                            :target :nodejs}}}})
