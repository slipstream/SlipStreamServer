(def +version+ "3.6-SNAPSHOT")

(defn sixsq-repo [version edition]
  (let [nexus-url "http://nexus.sixsq.com/content/repositories/"
        repo-type (if (re-find #"SNAPSHOT" version)
                    "snapshots"
                    "releases")]
    (str nexus-url repo-type "-" edition "-rhel7")))

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamServerPRSlib-jar
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community")

(set-env!
  :source-paths #{"resources" "test/clj"}
  :resource-paths #{"src/clj"}

  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-repo (get-env :version) (get-env :edition))}]])

  :dependencies
  '[[org.clojure/clojure "1.8.0" :scope "provided"]
    [org.clojure/tools.logging "0.3.1" :scope "provided"]
    [org.clojure/data.json "0.2.6" :scope "provided"]
    [com.sixsq.slipstream/SlipStreamPersistence "3.6-SNAPSHOT" :scope "provided"]
    [com.sixsq.slipstream/SlipStreamClientAPI-uber "3.6-SNAPSHOT" :scope "test"]
    [adzerk/boot-test "1.1.0" :scope "test"]
    [adzerk/boot-reload "0.4.5" :scope "test"]
    [tolitius/boot-check "0.1.1" :scope "test"]
    [sixsq/boot-deputil "0.2.2" :scope "test"]
    [boot-codox "0.9.5" :scope "test"]])

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[sixsq.boot-deputil :refer [set-deps!]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[codox.boot :refer [codox]])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  checkout {:dependencies [['sixsq/default-deps (get-env :version)]]}
  codox {:name         (str (get-env :project))
         :version      (get-env :version)
         :source-paths #{"src/clj"}
         :source-uri   "https://github.com/slipstream/SlipStreamServer/blob/master/jar-prslib/{filepath}#L{line}"
         :language     :clojure}
  test {:junit-output-to ""}
  )

(deftask run-tests
         "runs all tests and performs full compilation"
         []
         (comp
           (aot :all true)
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
           (install)
           (target)))

(deftask mvn-deploy
         "build full project through maven"
         []
         (comp
           (mvn-build)
           (push :repo "sixsq")))

(deftask setup-deps
         "setup dependencies for project"
         []
         (comp (checkout) (set-deps!)))

(boot (setup-deps))
