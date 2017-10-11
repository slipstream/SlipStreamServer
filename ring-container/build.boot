(def +version+ "3.38-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/slipstream-ring-container

  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-beta2"]
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

                   [org.clojure/tools.logging]
                   [aleph]
                   [environ]
                   [log4j]
                   [org.slf4j/slf4j-log4j12]

                   ;; boot tasks
                   [boot-environ]
                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [onetom/boot-lein-generate]
                   [tolitius/boot-check]]))))

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
  :source-paths #{"test" "test-resources"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))
        :repo "sixsq"})

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj"
                            #"log4j.properties"}
                 :invert true)
           (aot :namespace #{'sixsq.slipstream.server.ring-container})
           (jar)))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
           (build)
           (install)
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))
