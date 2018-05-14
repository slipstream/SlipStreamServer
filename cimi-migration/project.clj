(def +version+ "3.51-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamMigration-jar "3.51-SNAPSHOT"

  :distribution "utilities to manage with migrations"
  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.5"]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :pom-location "target/"

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.cli]
   [com.taoensso/timbre]]

  :profiles
  {
   :dbinit {:main "com.sixsq.slipstream.tools.cli.dbinit"
            :dependencies
                  [[com.sixsq.slipstream/SlipStreamDbBinding-jar ~+version+]
                   [com.sixsq.slipstream/SlipStreamDbSerializers-jar ~+version+]

                   ;; community
                   [com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
                   [com.sixsq.slipstream/SlipStreamServer-cimi-resources ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-OpenStack-conf ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-CloudStack-conf ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-OpenNebula-conf ~+version+]

                   ;; enterprise
                   [com.sixsq.slipstream/SlipStreamConnector-Exoscale-conf ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-NuvlaBox-conf ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-OTC-conf ~+version+]
                   [com.sixsq.slipstream/SlipStreamConnector-EC2-conf ~+version+]
                   ]}

   :dbcopy {:main "com.sixsq.slipstream.tools.cli.dbcopy"
            :dependencies
                  [[cc.qbits/spandex]]}
   })
