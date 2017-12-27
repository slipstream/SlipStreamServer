(def +version+ "3.42-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/token
  "3.42-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-environ "1.1.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.42-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories]}

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
             :dev  {:env {:config-name "config-hsqldb-mem.edn"}}}
  )