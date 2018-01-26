(def +version+ "3.45-SNAPSHOT")

(defproject
  com.sixsq.slipstream/token
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-environ "1.1.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src"]

  :dependencies
  [[org.clojure/clojure]
   [buddy/buddy-core]
   [buddy/buddy-hashers]
   [buddy/buddy-sign]
   [environ]
   [log4j]
   [org.clojure/tools.logging]
   [peridot]]

  :profiles {:test {:source-paths   ["test"]
                    :resource-paths ["test-resources"]}
             :dev  {:env {:config-name "config-hsqldb-mem.edn"}}})
