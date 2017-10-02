(def +version+ "3.38-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/utils
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
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
                  '[[org.clojure/clojure]

                    [log4j]
                    [org.clojure/data.json]
                    [org.clojure/tools.logging]
                    [org.clojure/java.jdbc]
                    [ring/ring-core]

                    [adzerk/boot-test]
                    [adzerk/boot-reload]
                    [tolitius/boot-check]
                    [onetom/boot-lein-generate]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni
                                with-eastwood
                                with-kibit
                                with-bikeshed]]
  '[boot.lein :refer [generate]])

(set-env! :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  push {:repo "sixsq"})

(deftask dev-env
         []
         (set-env! :source-paths #(set (concat % #{"test"})))
         identity)

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (dev-env)
           (aot :all true)
           (test)))

(deftask build []
         (comp
           (pom)
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
