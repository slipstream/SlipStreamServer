(def +version+ "3.47-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamDbSerializers-jar "3.47-SNAPSHOT"

  :description "utilities for serializing objects to a database"
  
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

  :pom-location "target/"

  :aot :all

  :dependencies [[org.clojure/clojure]
                 [environ]
                 [cheshire]
                 [camel-snake-kebab]
                 [me.raynes/fs]
                 [org.clojure/data.xml]
                 [superstring]
                 [org.clojure/test.check]
                 [com.sixsq.slipstream/SlipStreamPersistence ~+version+]
                 [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]]

  :profiles
  {:test     {:source-paths   ["test"]
              :resource-paths ["test-resources"]}
   :provided {:dependencies [[com.sixsq.slipstream/SlipStreamServer-cimi-resources ~+version+]]}})
