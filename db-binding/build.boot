(def +version+ "3.38-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamDbBinding-jar

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

                   [environ]

                   ; FIXME: needed this one after requiring
                   ; com.sixsq.slipstream.ssclj.middleware.authn-info-header
                   [cheshire]

                   [org.clojure/data.xml]
                   [clj-time]
                   [me.raynes/fs]
                   [org.clojure/data.json]
                   [org.clojure/tools.logging]
                   [org.elasticsearch/elasticsearch]
                   ; required by elasticsearch
                   [org.apache.logging.log4j/log4j-core]
                   [org.apache.logging.log4j/log4j-api]
                   [org.apache.logging.log4j/log4j-web]
                   [org.elasticsearch.client/transport]
                   [ring/ring-json]
                   [superstring]

                   ;;
                   ;; This dependency is included explicitly to avoid having
                   ;; ring/ring-json pull in an old version of ring-core that
                   ;; conflicts with the more recent one.
                   ;; 
                   [ring/ring-core]


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
  :source-paths #{"test"}
  :resource-paths #{"src" "resources"})

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
           (test)
           #_(sift :include #{#".*_test\.clj"}
                 :invert true)
           #_(aot :all true)))

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj"}
                 :invert true)
           (aot :all true)
            #_(uber :exclude #{ #"(?i)^META-INF/INDEX.LIST$"
                               #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                               #".*log4j\.properties" })
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
