(def +version+ "3.48-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamServerPRSlib-jar "3.48-SNAPSHOT"

  :description "Placement and Ranking Service"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [kirasystems/lein-codox "0.10.4"]
            [lein-shell "0.5.0"]
            [lein-localrepo "0.5.4"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.0.1"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

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
                 [com.sixsq.slipstream/SlipStreamPersistence ~+version+]
                 [com.sixsq.slipstream/SlipStreamConnector ~+version+]
                 [com.sixsq.slipstream/SlipStreamClojureAPI-cimi ~+version+]
                 [org.clojure/data.json]
                 [org.clojure/tools.logging]]

  :profiles {:test
             {:dependencies [[com.sixsq.slipstream/SlipStreamDbBinding-jar ~+version+]
                             [com.sixsq.slipstream/SlipStreamDbSerializers-jar ~+version+]
                             [com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
                             [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]]
              :resource-paths ["test-resources"]
              :jvm-opts ["-Djava.util.logging.config.file=test-resources/logging.properties"]}}

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
