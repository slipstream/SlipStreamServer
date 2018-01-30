(def +version+ "3.45-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamAsync "3.45-SNAPSHOT"

  :description "utilities for asynchronous actions"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/core.async]
   [com.sixsq.slipstream/SlipStreamPersistence]
   [com.sixsq.slipstream/SlipStreamConnector]]

  :profiles
  {:provided {:dependencies [[com.sixsq.slipstream/SlipStreamCljResources-jar]
                             [com.sixsq.slipstream/SlipStreamDbTesting-jar]]}})
