(defproject com.sixsq.slipstream/auth "2.16-SNAPSHOT"
  :description  "Authentication Service"
  :url          "http://sixsq.com"

  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :resource-paths ["resources"
                   "test/resources"]

  :dependencies [[org.clojure/clojure                       "1.7.0"]

                 ;; buddy
                 [buddy/buddy-core                          "0.6.0"]
                 [buddy/buddy-hashers                       "0.6.0"]
                 [buddy/buddy-sign                          "0.6.0"]

                 ;; logging
                 [org.clojure/tools.logging                 "0.3.0"]
                 [log4j/log4j                               "1.2.17"
                                        :exclusions [ javax.mail/mail
                                                      javax.jms/jms
                                                      com.sun.jdmk/jmxtools
                                                      com.sun.jmx/jmxri]]
                 ;; Environment settings
                 [environ                                   "1.0.0"]

                 ;; database
                 [org.clojure/java.jdbc                     "0.3.7"]
                 [korma                                     "0.4.2"]
                 [org.hsqldb/hsqldb                         "2.3.2"]
                 [org.xerial/sqlite-jdbc                    "3.7.2"]

                 ;; web
                 [http-kit/http-kit                         "2.1.18"]
                 [javax.servlet/servlet-api                 "2.5"]
                 [ring/ring-core                            "1.3.0"]
                 [ring/ring-json                            "0.3.1"]
                 [compojure                                 "1.4.0"]
                 [hiccup                                    "1.0.5"]]

  :plugins [[lein-environ "1.0.0"]]

  :profiles {
             :uberjar  {  :aot [#"com.sixsq.slipstream.auth.*"]
                          :env {  :clj-env        :production
                                  :config-path "config-hsqldb.edn" }
                          :jvm-opts ["-Dlogfile.path=production"]}


             :provided {:dependencies [[reply/reply "0.3.4"]]}

             :dev      {  :env          { :clj-env        :development
                                          :config-path "config-hsqldb.edn"
                                          :passphrase  "b8ddy-pr0t0"}
                          :jvm-opts     ["-Dlogfile.path=development"]
                          :dependencies [[peridot/peridot "0.3.0"]]}

             :test     {  :env          {:clj-env     :test
                                         :config-path "config-hsqldb-mem.edn"
                                         :passphrase  "b8ddy-pr0t0"}
                          :jvm-opts     ["-Dlogfile.path=test"]
                          :dependencies [[peridot/peridot "0.3.0"]]}})
