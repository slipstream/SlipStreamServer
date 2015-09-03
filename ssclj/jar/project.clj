(defproject com.sixsq.slipstream/ssclj "2.10.0-SNAPSHOT"
  :description    "Clojure REST resources"
  :url            "http://sixsq.com"
  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :source-paths   ["src/main/clojure"]  
  :test-paths     ["src/test/clojure"]
  :resource-paths ["src/main/resources"]

  :dependencies [[org.clojure/clojure                       "1.7.0"]
                 [org.clojure/tools.logging                 "0.3.0"]
                 [org.clojure/tools.namespace               "0.2.5"]
                 [org.clojure/tools.cli                     "0.3.1"]
                 [org.clojure/data.json                     "0.2.5"]
                 [prismatic/schema                          "0.2.6"]
                 [org.clojure/java.classpath                "0.2.2"]
                 [http-kit/http-kit                         "2.1.18"]
                 [clojurewerkz/scrypt                       "1.2.0"]
                 [ring/ring-core                            "1.3.0"]
                 [ring/ring-json                            "0.3.1"]
                 [javax.servlet/servlet-api                 "2.5"]
                 [compojure/compojure                       "1.1.8"]
                 [clj-time/clj-time                         "0.8.0"]
                 [clj-stacktrace/clj-stacktrace             "0.2.7"]
                 [metrics-clojure/metrics-clojure           "2.1.0"]
                 [metrics-clojure-ring/metrics-clojure-ring "2.1.0"]
                 [metrics-clojure-jvm/metrics-clojure-jvm   "2.1.0"]
                 [fs/fs                                     "1.3.3"]
                 [org.slf4j/slf4j-log4j12                   "1.7.7"]
                 [instaparse                                "1.4.0"]
                 ;; Environment settings
                 [environ                                   "1.0.0"]
                 ;; database
                 [honeysql                                  "0.5.2"]
                 [org.clojure/java.jdbc                     "0.3.6"]
                 [korma                                     "0.4.2"]
                 [org.hsqldb/hsqldb                         "2.3.2"]
                 [org.xerial/sqlite-jdbc                    "3.7.2"]]

  :plugins      [[lein-expectations                         "0.0.7"]
                 [lein-autoexpect                           "1.4.2"]
                 [lein-environ                              "1.0.0"]
                 [com.jakemccrary/lein-test-refresh         "0.5.5"]
                 [lein-kibit                                "0.1.2"]
                 [jonase/eastwood                           "0.2.1"]
                 [lein-cloverage                            "1.0.3"]]

  :profiles {
    :provided 
      { :dependencies [[reply/reply "0.3.4"]]}

    :uberjar 
      { :aot [#"com.sixsq.slipstream.ssclj.api.acl*"]
        :env {  :clj-env        :production
                :db-config-path "config-hsqldb-mem.edn"}
        :jvm-opts ["-Dlogfile.path=production"]}

    :dev 
      { :env {  :clj-env        :development
                :db-config-path "config-hsqldb-mem.edn"}
        :jvm-opts ["-Dlogfile.path=development"]
        :dependencies [ [peridot/peridot "0.3.0"]
                        [expectations/expectations "2.0.9"]]}
    :test
      { :env {  :clj-env        :test
                :db-config-path "config-hsqldb-mem.edn"}
        
        :jvm-opts ["-Dlogfile.path=test"]

        :dependencies [ [peridot/peridot "0.3.0"]
                        [expectations/expectations "2.0.9"]]}})


  
