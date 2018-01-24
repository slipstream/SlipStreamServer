(def +version+ "3.44")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamDbBinding-jar
  "3.44"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.44"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :test-paths ["test"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [environ]
   [cheshire]
   [org.clojure/data.xml]
   [clj-time]
   [me.raynes/fs]
   [org.clojure/data.json]
   [org.clojure/tools.logging]
   [org.elasticsearch/elasticsearch]
   [org.apache.logging.log4j/log4j-core]
   [org.apache.logging.log4j/log4j-api]
   [org.apache.logging.log4j/log4j-web]
   [org.elasticsearch.client/transport]
   [ring/ring-json]
   [superstring]
   [ring/ring-core]])
