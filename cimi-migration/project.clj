(def +version+ "3.70")

(defproject com.sixsq.slipstream/SlipStreamMigration-jar "3.70"

  :distribution "utilities to manage with migrations"
  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.19"]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :pom-location "target/"

  :main com.sixsq.slipstream.tools.cli.users-migration

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.cli]
   [com.taoensso/timbre]
   [com.sixsq.slipstream/SlipStreamDbBinding-jar ~+version+]
   [com.sixsq.slipstream/SlipStreamDbSerializers-jar ~+version+]
   [com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
   [com.sixsq.slipstream/SlipStreamServer-cimi-resources ~+version+]
   ]

  )
