(defproject com.sixsq.slipstream/auth "3.1-SNAPSHOT"
  :description  "Authentication Service"
  :url          "http://sixsq.com"

  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :resource-paths ["resources"
                   "test/resources"]

  :dependencies [[org.clojure/clojure                       "1.8.0"]
                 [org.clojure/data.json                     "0.2.6"]
                 [superstring                               "2.1.0"]

                 ;; buddy
                 [buddy/buddy-core                          "0.9.0"]
                 [buddy/buddy-hashers                       "0.9.0"]
                 [buddy/buddy-sign                          "0.9.0"]

                 ;; HTTP
                 [clj-http                                  "2.0.0"]

                 ;; logging
                 [org.clojure/tools.logging                 "0.3.1"]
                 [log4j/log4j                               "1.2.17"
                                        :exclusions [ javax.mail/mail
                                                      javax.jms/jms
                                                      com.sun.jdmk/jmxtools
                                                      com.sun.jmx/jmxri]]
                 ;; Environment settings
                 [environ                                   "1.0.1"]

                 ;; database
                 [org.clojure/java.jdbc                     "0.4.2"]
                 [korma                                     "0.4.2"]
                 [org.hsqldb/hsqldb                         "2.3.3"]
                 [org.xerial/sqlite-jdbc                    "3.8.11.2"]

                 ]

  :plugins [[lein-environ "1.0.0"]]

  :profiles {
             :uberjar  {  :aot [#"com.sixsq.slipstream.auth.*"]
                          :env {  :clj-env        :production
                                  :config-path "config-hsqldb-mem.edn" }
                          :jvm-opts ["-Dlogfile.path=production"]}


             :provided {:dependencies [[reply/reply "0.3.7"]]}

             :dev      {  :env          { :clj-env        :development
                                          :config-path "config-hsqldb-mem.edn"
                                          :passphrase  "sl1pstre8m"}
                          :jvm-opts     ["-Dlogfile.path=development"]
                          :dependencies [[peridot/peridot "0.3.0"]]}

             :test     {  :env          {:clj-env     :test
                                         :config-path "config-hsqldb-mem.edn"
                                         :passphrase  "sl1pstre8m"}
                          :jvm-opts     ["-Dlogfile.path=test"]
                          :dependencies [[peridot/peridot "0.3.0"]]}})
