(def +version+ "3.24")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamCljResources-jar

  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
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
                   [potemkin]
                   [prismatic/schema]
                   [ring/ring-core]
                   [ring/ring-json]
                   [superstring]

                   [com.sixsq.slipstream/auth]
                   [com.sixsq.slipstream/token]
                   [com.sixsq.slipstream/SlipStreamDbBinding-jar]
                   [org.apache.logging.log4j/log4j-core]
                   [org.apache.logging.log4j/log4j-api]

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
  :resource-paths #{"src" "resources"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))
        :repo "sixsq"})

(deftask dev-env
         []
         (set-env! :source-paths #(set (concat % #{"test" "test-resources"})))
         identity)

(deftask dev-fixture-env
         []
         (environ :env {:config-name      "config-hsqldb-mem.edn"
                        :auth-private-key (str (clojure.java.io/resource "auth_privkey.pem"))
                        :auth-public-key  (str (clojure.java.io/resource "auth_pubkey.pem"))}))

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (dev-env)
           (dev-fixture-env)
           (test)
           (sift :include #{#".*_test\.clj"
                            #".*test_utils\.clj"
                            #"test_helper\.clj"
                            #".*seeds.*"
                            #".*example\.clj"}
                 :invert true)
           (aot :all true)))

(defn get-file-path
  [fileset fname]
  (try
    (-> (-> fileset
          (boot.core/tmp-get fname)
          boot.core/tmp-dir)
      (clojure.java.io/file fname)
      .getPath)
    (catch IllegalArgumentException e)))

(deftask set-version []
  (fn middleware [next-task]
    (fn handler [fileset]
     (let [f (get-file-path fileset "com/sixsq/slipstream/version.txt")]
       (spit f (get-env :version)))
     (next-task fileset))))

(deftask build []
         (comp
           (pom)
           (set-version)
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

(def tests-artef-name "SlipStreamCljResourcesTests-jar")
(def tests-artef-pom-loc (str "com.sixsq.slipstream/" tests-artef-name))
(def tests-artef-project-name (symbol tests-artef-pom-loc))
(def tests-artef-jar-name (str tests-artef-name "-" (get-env :version) "-tests.jar"))

(deftask build-tests-jar
  "build jar with test runtime dependencies for connectors."
  []
  (comp
    (pom :project tests-artef-project-name :classifier "tests")
    (sift
      :to-resource #{#"lifecycle_test_utils\.clj"
                     #"connector_test_utils\.clj"}

      :include #{#"lifecycle_test_utils\.clj"
                 #"connector_test_utils\.clj"
                 #"pom.xml"
                 #"pom.properties"})

    (jar :file tests-artef-jar-name)))

(deftask mvn-test
         "run all tests of project"
         []
         (run-tests))

(deftask mvn-build
         "build project"
         []
         (comp
           (build)
           (install)
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))

(deftask mvn-build-tests-jar
         "build project"
         []
         (comp
          (dev-env)
          (build-tests-jar)
          (install :pom tests-artef-pom-loc)
          (if (= "true" (System/getenv "BOOT_PUSH"))
            (push :pom tests-artef-pom-loc)
            identity)))

(deftask server-repl
  "start dev server repl"
  []
  (comp
    (dev-env)
    (dev-fixture-env)
    (repl)))
