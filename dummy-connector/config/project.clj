(def +version+ "3.47")

(defproject com.sixsq.slipstream/SlipStreamConnector-Dummy-conf "3.47"

  :description "dummy connector for testing"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.0.1"]
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
   {:dependencies [[com.sixsq.slipstream/SlipStreamCljResourcesTests-jar ~+version+]
                   [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]
                   [peridot]
                   [commons-logging]
                   [org.clojure/test.check]
                   [org.slf4j/slf4j-log4j12]]
    :resource-paths ["test-resources"]}
   :provided
   {:dependencies [[com.sixsq.slipstream/SlipStreamServer-cimi-resources ~+version+]]}})

