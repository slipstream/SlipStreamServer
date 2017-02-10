(def +version+ "3.22-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamServerPRSlib-jar
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

                   [com.sixsq.slipstream/SlipStreamPersistence]
                   [com.sixsq.slipstream/SlipStreamConnector]
                   [com.sixsq.slipstream/SlipStreamDbSerializers-jar]
                   [com.sixsq.slipstream/SlipStreamClientAPI-jar]
                   [org.clojure/data.json]
                   [org.clojure/tools.logging]

                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]
                   [boot-codox]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[codox.boot :refer [codox]])

(set-env!
  :source-paths #{"test/clj"}
  :resource-paths #{"src/clj"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  codox {:name         (str (get-env :project))
         :version      (get-env :version)
         :source-paths #{"src/clj"}
         :source-uri   "https://github.com/slipstream/SlipStreamServer/blob/master/jar-prslib/{filepath}#L{line}"
         :language     :clojure}
  test {:junit-output-to ""}
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
           (sift :include #{#".*_test\.clj"}
                 :invert true)
           (aot :all true)
           (uber)
           (jar)))

(deftask mvn-test
         "run all tests of project"
         []
         (run-tests))

(deftask docs
         "builds API documentation and puts into target"
         []
         (comp
           (codox)
           (sift :include #{#"^doc.*"})
           (target)))

(deftask publish
         "publish API documentation to GitHub pages branch"
         []
         (fn middleware [next-handler]
           (fn handler [fileset]
             (require 'clojure.java.shell)
             (let [sh (resolve 'clojure.java.shell/sh)
                   result (sh "../publish-docs.sh")]
               (if (zero? (:exit result))
                 (next-handler fileset)
                 (throw (ex-info "Publishing docs failed!" result)))))))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
          (build)
          (install :pom (str (get-env :project)))
          (if (= "true" (System/getenv "BOOT_PUSH"))
            (push)
            identity)))
