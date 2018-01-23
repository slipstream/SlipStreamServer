(def +version+ "3.44-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/slipstream-ring-container
  "3.44-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.44-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :pom-location "target/"

  :aot [sixsq.slipstream.server.ring-container]

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.logging]
   [aleph]
   [environ]
   [log4j]
   [org.slf4j/slf4j-log4j12]]

  :profiles
  {:test     {:source-paths   ["test"]
              :resource-paths ["test-resources"]}})
