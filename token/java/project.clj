(def +version+ "3.72-SNAPSHOT")

(defproject com.sixsq.slipstream/token-java "3.72-SNAPSHOT"

  :description "token management utilities for java"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.19"]
                             :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["../clj/src"]

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/data.json]
   [buddy/buddy-core]
   [buddy/buddy-hashers]
   [buddy/buddy-sign]
   [environ]
   [log4j]
   [org.clojure/tools.logging]
   [peridot]])

