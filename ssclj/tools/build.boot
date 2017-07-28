(def +version+ "3.34-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamToolsCli-jar

  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
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
                  '[[org.clojure/clojure :scope "compile"]
                    [org.clojure/data.xml]
                    [org.clojure/tools.cli]

                    ;; boot tasks
                    [boot-environ]
                    [adzerk/boot-test]
                    [adzerk/boot-reload]
                    [tolitius/boot-check]

                    [com.sixsq.slipstream/SlipStreamPersistence nil :scope "compile"]
                    [com.sixsq.slipstream/SlipStreamDbSerializers-jar nil :scope "compile"]

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
                                with-bikeshed]])

(set-env!
  :source-paths #{"test"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))
        :repo "sixsq"})

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (test)
           (sift :include #{#".*_test\.clj"}
                 :invert true)
           (aot :all true)))

(deftask build []
         (comp
           (pom)
           (aot :namespace #{'com.sixsq.slipstream.tools.cli.ssconfig
                             'com.sixsq.slipstream.tools.cli.ssconfigdump
                             'com.sixsq.slipstream.tools.cli.ssconfigmigrate})
           (jar)))

(deftask build-uberjar []
         (comp
           (pom)
           (aot :namespace #{'com.sixsq.slipstream.tools.cli.ssconfig
                             'com.sixsq.slipstream.tools.cli.ssconfigdump
                             'com.sixsq.slipstream.tools.cli.ssconfigmigrate})
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
