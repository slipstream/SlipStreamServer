(def +version+ "3.43-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/utils
  "3.43-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.43-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :pom-location "target/"

  :source-paths ["src"]

  :dependencies
  [[org.clojure/clojure]
   [log4j]
   [org.clojure/data.json]
   [org.clojure/tools.logging]
   [org.clojure/java.jdbc]
   [ring/ring-core]]

  :profiles {:test {:aot            :all
                    :source-paths   ["test"]
                    :resource-paths ["test-resources"]}})
