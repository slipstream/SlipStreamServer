(def +version+ "3.41-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamAsync
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
                 '[[org.clojure/clojure]
                   [org.clojure/core.async]

                   [com.sixsq.slipstream/SlipStreamPersistence]
                   [com.sixsq.slipstream/SlipStreamConnector]

                   [com.sixsq.slipstream/SlipStreamDbSerializers-jar nil :scope "test"]
                   [com.sixsq.slipstream/SlipStreamCljResources-jar nil :scope "provided"]
                   [com.sixsq.slipstream/SlipStreamCljResourcesTestServer-jar :classifier "tests" :scope "test"]

                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]
                   [boot-codox]
                   [onetom/boot-lein-generate]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[codox.boot :refer [codox]]
  '[boot.lein :refer [generate]])

(set-env!
  :source-paths #{"test"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))
        :repo "sixsq"})

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (test)))

(deftask build []
         (comp
           (pom)
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
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))
