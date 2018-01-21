(def +version+ "3.43")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamServerPRSlib-jar
  "3.43"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]
            [kirasystems/lein-codox "0.10.4"]
            [lein-shell "0.5.0"]
            [lein-localrepo "0.5.4"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.43"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["src/clj"]

  :test-paths ["test/clj"]

  :pom-location "target/"

  :aot :all

  :codox {:name         "com.sixsq.slipstream/SlipStreamServerPRSlib-jar"
          :version      ~+version+
          :source-paths #{"src/clj"}
          :source-uri   "https://github.com/slipstream/SlipStreamServer/blob/master/jar-prslib/{filepath}#L{line}"
          :language     :clojure}

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
             {:dependencies [[com.sixsq.slipstream/SlipStreamDbBinding-jar]
                             [com.sixsq.slipstream/SlipStreamDbSerializers-jar]
                             [com.sixsq.slipstream/SlipStreamCljResources-jar]
                             [com.sixsq.slipstream/SlipStreamDbTesting-jar]]}}

  :aliases {"install" [["do"
                        ["uberjar"]
                        ["pom"]
                        ["localrepo" "install" "-p" "target/pom.xml"
                         ~(str "target/SlipStreamServerPRSlib-jar-" +version+ "-standalone.jar")
                         "com.sixsq.slipstream/SlipStreamServerPRSlib-jar"
                         ~+version+]
                        ]]
            "docs"    ["codox"]
            "publish" ["shell" "../publish-docs.sh"]})
