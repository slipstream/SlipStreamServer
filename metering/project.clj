(def +version+ "3.45-SNAPSHOT")

(defproject
  com.sixsq.slipstream/metering
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-shell "0.5.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src"]
  :resource-paths ["resources"]

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.logging]
   [org.clojure/data.json]
   [log4j]
   [com.sixsq.slipstream/SlipStreamClojureAPI-cimi]
   [aleph]
   [cc.qbits/spandex]
   [environ]
   [compojure]
   [ring/ring-json]
   [ring/ring-defaults]]

  :profiles {:test {:dependencies [[com.sixsq.slipstream/slipstream-ring-container]
                                   [com.sixsq.slipstream/SlipStreamDbTesting-jar]]
                    :source-paths ["test"]
                    :resource-paths ["test-resources"]}
             :provided {:dependencies [[org.slf4j/slf4j-log4j12]]}})
