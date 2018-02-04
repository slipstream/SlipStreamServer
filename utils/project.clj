(def +version+ "3.46-SNAPSHOT")

(defproject com.sixsq.slipstream/utils "3.46-SNAPSHOT"

  :description "general server utilities"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.46-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src"]

  :dependencies
  [[org.clojure/clojure]
   [ring/ring-core]]

  :profiles {:test {:aot            :all
                    :source-paths   ["test"]
                    :resource-paths ["test-resources"]}})
