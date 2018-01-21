(def +version+ "3.43")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamAsync
  "3.43"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.43"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/core.async]
   [com.sixsq.slipstream/SlipStreamPersistence]
   [com.sixsq.slipstream/SlipStreamConnector]]

  :profiles
  {:provided {:dependencies [[com.sixsq.slipstream/SlipStreamCljResources-jar]
                             [com.sixsq.slipstream/SlipStreamDbTesting-jar]]}})
