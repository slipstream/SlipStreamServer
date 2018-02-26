(def +version+ "3.46")

(defproject com.sixsq.slipstream/SlipStreamDbBinding-jar "3.46"

  :description "bindings for (persistent) database backends"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.46"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :test-paths ["test"]

  :pom-location "target/"

  :dependencies
  [[cc.qbits/spandex]
   [org.clojure/tools.reader]                               ;; required by spandex through core.async
   [cheshire]                                               ;; to avoid transient dependency conflicts
   [clj-time]
   [com.rpl/specter]
   [com.sixsq.slipstream/utils]
   [duratom]
   [environ]
   [org.apache.logging.log4j/log4j-core]                    ;; required for Elasticsearch logging
   [org.clojure/data.json]
   [org.clojure/tools.logging]
   [org.elasticsearch/elasticsearch]
   [org.elasticsearch.client/transport]
   [org.slf4j/slf4j-api]
   [ring/ring-json]
   [superstring]                                            ;; needed for pascal case conversion function
   ]

  :profiles {:test     {:aot            :all
                        :resource-paths ["test-resources"]
                        :dependencies   [[org.slf4j/slf4j-log4j12]
                                         [me.raynes/fs]
                                         [org.elasticsearch.test/framework]]}
             :provided {:dependencies [[org.clojure/clojure]]}})
