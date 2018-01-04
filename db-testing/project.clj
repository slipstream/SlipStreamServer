(def +version+ "3.42-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))


(defproject
  com.sixsq.slipstream/SlipStreamDbTesting-jar
  "3.42-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.42-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories :plugins]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :test-paths ["test"]

  :java-source-paths ["java"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   ;[org.clojure/tools.reader]

   [environ]

   ; FIXME: needed this one after requiring
   ; com.sixsq.slipstream.ssclj.middleware.authn-info-header
   [cheshire]
   [cc.qbits/spandex]
   [org.clojure/data.xml]
   [clj-time]
   [me.raynes/fs]
   [org.clojure/data.json]
   [org.clojure/tools.logging]
   [org.clojure/tools.reader]
   [org.elasticsearch/elasticsearch]
   ; required by elasticsearch
   [org.apache.logging.log4j/log4j-core]
   [org.apache.logging.log4j/log4j-api]
   [org.apache.logging.log4j/log4j-web]
   [org.elasticsearch.client/transport]
   [org.elasticsearch.plugin/transport-netty4-client]
   [org.elasticsearch.test/framework]

   [ring/ring-json]
   [superstring]

   [com.sixsq.slipstream/SlipStreamDbBinding-jar]
   ;;
   ;; This dependency is included explicitly to avoid having
   ;; ring/ring-json pull in an old version of ring-core that
   ;; conflicts with the more recent one.
   ;;
   [ring/ring-core]])
