(def +version+ "3.45-SNAPSHOT")

(defproject com.sixsq.slipstream/token-java "3.45-SNAPSHOT"

  :description "token management utilities for java"
  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["../clj/src"]

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [buddy/buddy-core]
   [buddy/buddy-hashers]
   [buddy/buddy-sign]
   [environ]
   [log4j]
   [org.clojure/tools.logging]
   [peridot]])

