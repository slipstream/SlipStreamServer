(def +version+ "3.41-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamToolsCli-jar

  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-beta2"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url]])

(set-env!
  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-nexus-url)}]])

  :dependencies
  #(vec (concat %
                (merge-defaults
                  ['sixsq/default-deps (get-env :version)]
                  '[[org.clojure/clojure :scope "compile"]
                    [org.clojure/data.xml]
                    [org.clojure/tools.cli]

                    ;; needed for migration scripts
                    [korma]
                    [org.hsqldb/hsqldb]
                    [org.clojure/java.jdbc]

                    ;; boot tasks
                    [boot-environ]
                    [adzerk/boot-test]
                    [adzerk/boot-reload]
                    [tolitius/boot-check]
                    [onetom/boot-lein-generate]

                    [com.sixsq.slipstream/SlipStreamPersistence nil :scope "compile" :exclusions [org.slf4j/slf4j-jdk14]]
                    [com.sixsq.slipstream/SlipStreamDbSerializers-jar nil :scope "compile"]

                    [com.sixsq.slipstream/SlipStreamCljResourcesTests-jar nil :classifier "tests" :scope "test"]

                    [superstring]
                    [me.raynes/fs]
                    [clj-http]]))))

(require
  '[environ.boot :refer [environ]]
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni
                                with-eastwood
                                with-kibit
                                with-bikeshed]]
  '[boot.lein :refer [generate]])

(set-env!
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))
        :repo "sixsq"})

(deftask test-paths
         []
         (set-env! :source-paths #(set (concat % #{"test" "test-resources"})))
         identity)

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (test-paths)
           (test)
           (sift :include #{#".*_test\.clj"}
                 :invert true)
           (aot :all true)))

(deftask build []
         (comp
           (pom)
           (aot :namespace #{'com.sixsq.slipstream.tools.cli.ssconfig
                             'com.sixsq.slipstream.tools.cli.ssconfigdump
                             'com.sixsq.slipstream.tools.cli.ssconfigmigrate
                             'com.sixsq.slipstream.ssclj.usage.summarizer})
           (jar)))

(deftask build-uberjar []
         (comp
           (pom)
           (aot :namespace #{'com.sixsq.slipstream.tools.cli.ssconfig
                             'com.sixsq.slipstream.tools.cli.ssconfigdump
                             'com.sixsq.slipstream.tools.cli.ssconfigmigrate
                             'com.sixsq.slipstream.ssclj.usage.summarizer})
           (uber)
           (jar)))

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
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))
