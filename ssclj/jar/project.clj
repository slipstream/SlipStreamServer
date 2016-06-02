(defproject com.sixsq.slipstream/ssclj "3.5-SNAPSHOT"
  :description    "Clojure REST resources"
  :url            "http://sixsq.com"
  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :source-paths   ["src/main/clojure"]
  :test-paths     ["src/test/clojure"]
  :resource-paths ["src/main/resources"]

  :dependencies [[org.clojure/clojure                       "1.8.0"]
                 [org.clojure/tools.logging                 "0.3.1"]
                 [org.clojure/tools.namespace               "0.2.10"]
                 [org.clojure/tools.cli                     "0.3.3"]
                 [org.clojure/data.json                     "0.2.6"]
                 [org.clojure/java.classpath                "0.2.3"]
                 [superstring                               "2.1.0"]
                 [prismatic/schema                          "1.0.3"]
                 [clojurewerkz/scrypt                       "1.2.0"]
                 [clj-time/clj-time                         "0.11.0"]
                 [clj-stacktrace/clj-stacktrace             "0.2.8"]
                 [metrics-clojure/metrics-clojure           "2.5.1"]
                 [metrics-clojure-ring/metrics-clojure-ring "2.5.1"
                   :exclusions [[cheshire/cheshire]] ]
                 [metrics-clojure-jvm/metrics-clojure-jvm   "2.5.1"]
                 [metrics-clojure-graphite/metrics-clojure-graphite   "2.5.1"]

                 [fs/fs                                     "1.3.3"]
                 [org.slf4j/slf4j-log4j12                   "1.7.13"]
                 [instaparse                                "1.4.1"]
                ; ;; Authentication service
                 [com.sixsq.slipstream/auth                 "3.5-SNAPSHOT"]
                ; ;; Environment settings
                 [environ                                   "1.0.1"]
                ; ;; database
                 [honeysql                                  "0.6.2"]
                 [org.clojure/java.jdbc                     "0.4.2"]
                 [korma                                     "0.4.2"]
                 [org.hsqldb/hsqldb                         "2.3.3"]
                 [org.xerial/sqlite-jdbc                    "3.8.11.2"]
                ; ;; http related
                 [javax.servlet/javax.servlet-api           "3.1.0"]
                 [ring/ring-core                            "1.4.0"]
                 [ring/ring-json                            "0.4.0"]
                 [compojure/compojure                       "1.4.0"]
                 [http-kit/http-kit                         "2.1.19"]
                 [clj-http                                  "2.0.0"]

                 [puppetlabs/http-client                    "0.4.5"
                  :exclusions [[cheshire/cheshire]] ]
                 [aleph                                     "0.4.1-beta4"]

                 ;; Elastic search
                 [org.elasticsearch/elasticsearch           "2.3.1"]
                 [me.raynes/fs                              "1.4.6"]]

  :plugins      [[lein-ancient                              "0.6.8"]
                 [lein-expectations                         "0.0.7"]
                 [lein-autoexpect                           "1.4.2"]
                 [lein-environ                              "1.0.0"]
                 [com.jakemccrary/lein-test-refresh         "0.5.5"]
                 [lein-kibit                                "0.1.2"]
                 [jonase/eastwood                           "0.2.1"]
                 [lein-cloverage                            "1.0.3"]]

  :repl-options {;; What to print when the repl session starts.
                :welcome
                (println (str
                  ;; These first lines are the default ones:
                  "\n"
                  "      Docs: (doc function-name-here)\n"
                  "            (find-doc \"part-of-name-here\")\n"
                  "    Source: (source function-name-here)\n"
                  "   Javadoc: (javadoc java-object-or-class-here)\n"
                  "      Exit: Control+D or (exit) or (quit)\n"
                  "   Results: Stored in vars *1, *2, *3, an exception in *e\n"
                  "\n"
                  ;; This line is related to the SlipStream project:
                  "API Server:      (require '[com.sixsq.slipstream.ssclj.app.server :as server])\n"
                  "                 (def state (server/start 8201))\n"
                  "Event Seeding:   (require '[com.sixsq.slipstream.ssclj.resources.seeds.event :as es])\n"
                  "                 (es/seed! 10 :bob)\n"
                  "Usage Seeding:   (require '[com.sixsq.slipstream.ssclj.usage.seeds.usages :as us])\n"
                  "                 (us/seed-summaries! 10 :bob [\"aws\" \"exo\"])\n"
                  ))
                }

 :profiles {
  ; :provided
  ;   { :dependencies [[reply/reply "0.3.4" :exlusions [[cheshire/cheshire]]]] }

   :uberjar
     { :aot [#"com.sixsq.slipstream.ssclj.api.acl*"]
       :env {  :clj-env        "production"
               :config-path    "config-hsqldb-mem.edn"}
       :jvm-opts ["-Dlogfile.path=production"]}

   :dev
     { :env {  :clj-env        "development"
               :config-path    "config-hsqldb.edn"}
       :jvm-opts ["-Dlogfile.path=development"]
       :dependencies [ [peridot/peridot "0.4.1"]
                       [expectations/expectations "2.1.4"]]}
   :test
     { :env {  :clj-env        "test"
               :config-path    "config-hsqldb-mem.edn"}

       :jvm-opts ["-Dlogfile.path=test"]

       :dependencies [ [peridot/peridot "0.4.1"]
                       [expectations/expectations "2.1.4"]]}
  }
  )
