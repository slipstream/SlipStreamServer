(def +version+ "3.45-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamCljResources-jar
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [lein-environ "1.1.0"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :pom-location "target/"

  :dependencies
  [[org.clojure/clojure]
   [aleph]
   [cheshire]                                               ;; newer version needed for ring-json
   [compojure]
   [com.jcraft/jsch]
   [clj-stacktrace]
   [clj-time]
   [environ]
   [expound]
   [instaparse]
   [log4j]
   [metrics-clojure]
   [metrics-clojure-ring]
   [metrics-clojure-jvm]
   [metrics-clojure-graphite]
   [me.raynes/fs]
   [org.apache.logging.log4j/log4j-core]
   [org.apache.logging.log4j/log4j-api]
   [org.clojure/data.json]
   [org.clojure/java.classpath]
   [org.clojure/tools.cli]
   [org.clojure/tools.logging]
   [org.clojure/tools.namespace]
   [potemkin]
   [ring/ring-core]
   [ring/ring-json]
   [superstring]
   [zookeeper-clj]
   [com.draines/postal]

   ; dependencies for auth
   [buddy/buddy-core]
   [buddy/buddy-hashers]
   [buddy/buddy-sign]
   [clj-http]
   [peridot]
   [com.sixsq.slipstream/utils]
   [com.sixsq.slipstream/SlipStreamDbBinding-jar]
   [com.sixsq.slipstream/SlipStreamClojureAPI-cimi]
   [com.sixsq.slipstream/SlipStreamCljResourcesTests-jar]
   [com.sixsq.slipstream/token]

   [org.clojure/test.check] ; FIXME: Needed for spec.gen.alpha. Fix "Could not locate clojure/test/check/generators__init.class"
   ]

  :aot [com.sixsq.slipstream.ssclj.app.main
        com.sixsq.slipstream.ssclj.util.userparamsdesc]

  :profiles
  {
   :provided {:aot [com.sixsq.slipstream.ssclj.app.main]}
   :test     {:dependencies   [[peridot]
                               [org.clojure/test.check]
                               [org.slf4j/slf4j-log4j12]
                               [com.cemerick/url]
                               [org.apache.curator/curator-test]
                               [com.sixsq.slipstream/SlipStreamDbTesting-jar]]
              :resource-paths ["test-resources"]
              :env            {:config-name      "config-params.edn"
                               :auth-private-key "test-resources/auth_privkey.pem"
                               :auth-public-key  "test-resources/auth_pubkey.pem"}
              #_:aot            #_:all}
   :dev      {:dependencies [[com.sixsq.slipstream/slipstream-ring-container]]}
   })
