(def +version+ "3.45-SNAPSHOT")

(defproject
  com.sixsq.slipstream/SlipStreamToolsCli-jar
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["src"]

  :test-paths ["test"]

  :pom-location "target/"

  :aot [com.sixsq.slipstream.tools.cli.ssconfig
        com.sixsq.slipstream.tools.cli.ssconfigdump
        com.sixsq.slipstream.tools.cli.ssconfigmigrate]

  :dependencies
  [[org.clojure/clojure :scope "compile"]
   [org.clojure/data.xml]
   [org.clojure/tools.cli]

   [com.sixsq.slipstream/SlipStreamPersistence :scope "compile"]
   [com.sixsq.slipstream/SlipStreamCljResources-jar :scope "compile"]
   [com.sixsq.slipstream/SlipStreamDbBinding-jar :scope "compile"]

   [com.sixsq.slipstream/SlipStreamDbSerializers-jar nil :scope "compile"]

   [superstring]
   [me.raynes/fs]
   [clj-http]]

  :profiles {:test {:dependencies [[com.sixsq.slipstream/SlipStreamDbTesting-jar]]
                    :aot          :all}})
