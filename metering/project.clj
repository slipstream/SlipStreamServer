(def +version+ "3.47")

(defproject com.sixsq.slipstream/metering "3.47"

  :description "metering server"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [lein-shell "0.5.0"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.0.1"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src"]
  :resource-paths ["resources"]

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.logging]
   [org.clojure/data.json]
   [log4j]
   [com.sixsq.slipstream/SlipStreamClojureAPI-cimi ~+version+]
   [aleph]
   [cc.qbits/spandex]
   [environ]
   [compojure]
   [ring/ring-json]
   [ring/ring-defaults]]

  :profiles {:test {:dependencies [[com.sixsq.slipstream/slipstream-ring-container ~+version+]
                                   [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]]
                    :source-paths ["test"]
                    :resource-paths ["test-resources"]}
             :provided {:dependencies [[org.slf4j/slf4j-log4j12]]}})
