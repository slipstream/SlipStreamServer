(def +version+ "3.42-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamServerPRSlib-jar
  "3.42-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.42-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories]}

  :source-paths ["src/clj"]

  :test-paths ["test/clj"]

  :pom-location "target/"

  :aot :all

  :uberjar-exclusions [#"(?i)^META-INF/INDEX.LIST$"
                       #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                       #"elasticsearch"]

  :dependencies [[org.clojure/clojure]
                 [com.sixsq.slipstream/SlipStreamPersistence]
                 [com.sixsq.slipstream/SlipStreamConnector]
                 [com.sixsq.slipstream/SlipStreamClientAPI-jar]
                 [org.clojure/data.json]
                 [org.clojure/tools.logging]]

  :profiles {:test
             {:dependencies [[sixsq/build-utils "0.1.4"]
                             [com.sixsq.slipstream/SlipStreamDbBinding-jar]
                             [com.sixsq.slipstream/SlipStreamDbSerializers-jar]
                             [com.sixsq.slipstream/SlipStreamCljResources-jar]]}}
  )