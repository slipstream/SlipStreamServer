(def +version+ "3.54-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamAsync "3.54-SNAPSHOT"

  :description "utilities for asynchronous actions"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.6"]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/core.async]
   [com.sixsq.slipstream/SlipStreamPersistence ~+version+]
   [com.sixsq.slipstream/SlipStreamConnector ~+version+]]

  :profiles
  {:provided {:dependencies [[com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
                             [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]]}})
