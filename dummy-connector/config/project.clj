(def +version+ "3.46-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamConnector-Dummy-conf "3.46-SNAPSHOT"

  :description "dummy connector for testing"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.46-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :test-paths ["test"]

  :pom-location "target/"

  :dependencies
  [[org.clojure/clojure]]

  :profiles
  {:test
   {:dependencies [[com.sixsq.slipstream/SlipStreamCljResourcesTests-jar]
                   [com.sixsq.slipstream/SlipStreamDbTesting-jar]
                   [peridot]
                   [commons-logging]
                   [org.clojure/test.check]
                   [org.slf4j/slf4j-log4j12]]
    :resource-paths ["test-resources"]}
   :provided
   {:dependencies [[superstring]
                   [com.sixsq.slipstream/SlipStreamCljResources-jar]]}})

