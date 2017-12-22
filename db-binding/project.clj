(def +version+ "3.42-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamDbBinding-jar
  "3.42-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.42-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories]}


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
   [ring/ring-core]]

  :profiles
  {:test
   {:dependencies [[sixsq/build-utils "0.1.4"]]
    :source-paths ["clj/test" "src/test"]}}
  )