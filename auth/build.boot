(def +version+ "3.18-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/auth
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
                   
                   [buddy/buddy-core]
                   [buddy/buddy-hashers]
                   [buddy/buddy-sign]
                   [clj-http]
                   [environ]
                   [korma]
                   [log4j]
                   [org.clojure/data.json]
                   [org.clojure/tools.logging]
                   [org.clojure/java.jdbc]
                   [org.hsqldb/hsqldb]
                   [superstring]

                   [peridot]

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
  )

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (environ :env {:config-path "config-hsqldb-mem.edn"
                         :passphrase "sl1pstre8m"})
           (test)))

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj" #"test_helper\.clj"}
                 :invert true)
           (aot :all true)
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
           (target)))

(deftask mvn-deploy
         "build full project through maven"
         []
         (comp
           (mvn-build)
           (push :repo "sixsq")))
