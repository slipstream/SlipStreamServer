(def +version+ "3.13")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamCljResources-jar

  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url
                                   lein-generate]])

(set-env!
  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-nexus-url)}]])

  :dependencies
  #(vec (concat %
                (merge-defaults
                 ['sixsq/default-deps (get-env :version)]
                 '[[org.clojure/clojure]

                   [aleph]
                   [cheshire] ;; newer version needed for ring-json
                   [compojure]
                   [clj-stacktrace]
                   [clj-time]
                   [environ]
                   [instaparse]
                   [log4j]
                   [metrics-clojure]
                   [metrics-clojure-ring]
                   [metrics-clojure-jvm]
                   [metrics-clojure-graphite]
                   [me.raynes/fs]
                   [org.clojure/data.json]
                   [org.clojure/java.classpath]
                   [org.clojure/tools.cli]
                   [org.clojure/tools.logging]
                   [org.clojure/tools.namespace]
                   [org.elasticsearch/elasticsearch]
                   [org.slf4j/slf4j-log4j12]
                   [potemkin]
                   [prismatic/schema]
                   [ring/ring-core]
                   [ring/ring-json]
                   [superstring]

                   [com.sixsq.slipstream/auth]
                   [com.sixsq.slipstream/SlipStreamDbBinding-jar]

                   ;; needed for migration scripts
                   [korma]
                   [org.hsqldb/hsqldb]
                   [org.clojure/java.jdbc]

                   ;; test dependencies
                   [peridot]
                   [expectations]
                   [honeysql]

                   ;; boot tasks
                   [boot-environ]
                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]]))))

(require
  '[environ.boot :refer [environ]]
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni
                                with-eastwood
                                with-kibit
                                with-bikeshed]])

(set-env!
  :source-paths #{"test" "test-resources"}
  :resource-paths #{"src" "resources"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))}
  )

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (environ :env {:config-path "config-hsqldb-mem.edn"
                          :passphrase "sl1pstre8m"})
           ;;(aot :all true)
           (test)

           (sift :include #{#".*_test\.clj"
                            #".*test_utils\.clj"
                            #"test_helper\.clj"
                            #".*seeds.*"
                            #".*example\.clj"}
                 :invert true)
           (aot :all true)))

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj"
                            #".*test_utils\.clj"
                            #"test_helper\.clj"
                            #".*seeds.*"
                            #".*example\.clj"
                            #".*Test\.java"
                            #".*simu_result.txt"
                            #"config-hsqldb-mem.edn"
                            #"config-hsqldb.edn"
                            #"log4j.properties"}
                 :invert true)
           (aot :namespace #{'com.sixsq.slipstream.ssclj.app.main 'com.sixsq.slipstream.ssclj.usage.summarizer})
           #_(uber :exclude #{ #"(?i)^META-INF/INDEX.LIST$"
                             #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                             #".*log4j\.properties" })
           (jar ;; :main 'com.sixsq.slipstream.ssclj.app.main
            )))

(deftask mvn-test
         "run all tests of project"
         []
         (run-tests))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
           (build)
           (install)
           (target)))

(deftask mvn-deploy
         "build full project through maven"
         []
         (comp
           (mvn-build)
           (push :repo "sixsq")))
