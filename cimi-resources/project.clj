(def +version+ "3.45-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamServer-cimi-resources "3.45-SNAPSHOT"

  :description "CIMI resources"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [lein-environ "1.1.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :profiles {:provided {:dependencies [[org.clojure/clojure]]}
             :test     {:aot :all}})
