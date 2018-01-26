(def +version+ "3.45-SNAPSHOT")

(defproject
  com.sixsq.slipstream/SlipStreamDbSerializers-jar
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

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
                 [com.sixsq.slipstream/SlipStreamPersistence]
                 [com.sixsq.slipstream/SlipStreamDbTesting-jar]]

  :profiles
  {:test     {:source-paths   ["test"]
              :resource-paths ["test-resources"]}
   :provided {:dependencies [[com.sixsq.slipstream/SlipStreamCljResources-jar]]}})
