(def +version+ "3.19-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/ssclj-service-catalog-jar
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
                   
                   [commons-logging]
                   [org.clojure/data.zip]

                   [com.sixsq.slipstream/SlipStreamCljResources-jar nil :scope "provided"]
                   [com.sixsq.slipstream/SlipStreamDbBinding-jar nil :scope "provided"]

                   [peridot]
                   [expectations]
                   
                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [seancorfield/boot-expectations]
                   [tolitius/boot-check]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[seancorfield.boot-expectations :refer :all]
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
          (test
           :exclusions #{'com.sixsq.slipstream.ssclj.resources.service-attribute-schema-test
                         'com.sixsq.slipstream.ssclj.resources.service-attribute-test
                         'com.sixsq.slipstream.ssclj.resources.service-offer-schema-test})
          (expectations)
          (sift :include #{#".*_test\.clj"
                           #"test_utils\.clj"}
                :invert true)
          (aot :all true)))

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj"
                            #"test_utils\.clj"
                            #"log4j.properties"}
                 :invert true)
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
