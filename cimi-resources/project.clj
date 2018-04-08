(def +version+ "3.49-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamServer-cimi-resources "3.49-SNAPSHOT"

  :description "CIMI resources"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [lein-environ "1.1.0"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.1.1"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :jvm-opts ["-Xmx2000m"]

  :dependencies [[com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
                 [com.amazonaws/aws-java-sdk-s3]
                 [org.clojure/test.check]                   ; FIXME: Needed for spec.gen.alpha. Fix "Could not locate clojure/test/check/generators__init.class"
                 ]

  :profiles {:provided {:dependencies [[org.clojure/clojure]]}
             :test     {:dependencies   [[peridot]
                                         [org.clojure/test.check]
                                         [org.slf4j/slf4j-log4j12]
                                         [com.cemerick/url]
                                         [org.apache.curator/curator-test]
                                         [com.sixsq.slipstream/SlipStreamDbTesting-jar ~+version+]
                                         [com.sixsq.slipstream/SlipStreamCljResourcesTests-jar ~+version+]
                                         ]
                        :resource-paths ["test-resources"]
                        :env            {:config-name      "config-params.edn"
                                         :auth-private-key "test-resources/auth_privkey.pem"
                                         :auth-public-key  "test-resources/auth_pubkey.pem"}
                        :aot            :all}})
