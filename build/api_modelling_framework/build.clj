(ns api-modelling-framework.build
  (:require [clojure.java.shell :as jsh]
            [clojure.string :as string]
            [cheshire.core :as json]))

;; Project information
(defn project-info [] (-> "project.clj" slurp read-string))
(defn find-project-info [kw] (->> (project-info) (drop-while #(not= % kw)) second))

(def version  (-> (project-info) (nth 2) (string/split #"-") first))
(def project  (-> (project-info) (nth 1) str))
(def description (find-project-info :description))
(def keywords ["raml" "open-api" "swagger" "rdf" "shacl" "api" "modelling"])
(def license  (-> (find-project-info :license) :name))
(def repository "https://github.com/mulesoft-labs/api-modelling-framework")
(def npm-dependencies (->> (find-project-info :npm) :dependencies (map (fn [[n v]] [(str n) (str v)]))(into {})))


;; Packages
(defn npm-package []
  {:name project
   :description description
   :version version
   :main "index"
   :license license
   :repository repository
   :dependencies npm-dependencies})

;; Commands
(defn sh! [& args]
  (println "==> " (string/join " " args))
  (let [{:keys [err out exit]} (apply jsh/sh args)]
    (if (not= exit 0)
      (throw (Exception. err))
      (clojure.string/split-lines out))))

(defn mkdir [path]
  (sh! "mkdir" "-p" path))

(defn rm [path]
  (sh! "rm" "-rf" path))

(defn cljsbuild [target]
  (sh! "lein" "cljsbuild" "once" target))

(defn clean []
  (sh! "lein" "clean"))

(defn cp [from to]
  (sh! "cp" "-rf" from to))

(defn pwd [] (first (sh! "pwd")))

(defn ln [source target]
  (sh! "ln" "-s" (str (pwd) source) (str (pwd) target)))


;; builds

(defn build [target]
  (println "* Cleaning output directory")
  (clean)
  (rm "target")
  (rm (str "output/" target))

  (println "* Recreating output directory")
  (mkdir "output")

  (println "* Building " target)
  (cljsbuild target)
  (cp "js" "output/node/js")

  (println "* Copying license")
  (cp "LICENSE" (str "output/" target "/LICENSE")))

;; CLI

(defn build-node []
  (println "** Building Target: node\n")
  (build "node")

  (println "* copy package index file")
  (cp "build/package_files/index.js" "output/node/index.js")

  (println "generating npm package")
  (-> (npm-package)
      (json/generate-string {:pretty true})
      (->> (spit "output/node/package.json"))))

(defn build-web []
  (println "** Building Target: web\n")
  (build "web"))


(defn -main [& args]
  (try
    (condp = (first args)
      "web" (build "web")
      "node" (build-node)
      (println "Unknown task"))
    (catch Exception ex
      (println "Error building project")
      (prn ex)
      (System/exit 1)))
  (System/exit 0))
