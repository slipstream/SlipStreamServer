(def +version+ "3.41-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/metering

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
                    [org.clojure/tools.logging]
                    [org.clojure/data.json]
                    [log4j]
                    [org.slf4j/slf4j-log4j12 :scope "provided"]

                    [com.sixsq.slipstream/slipstream-ring-container :scope "test"]
                    [com.sixsq.slipstream/SlipStreamClientAPI-jar]

                    [aleph]
                    [cc.qbits/spandex]
                    [environ]
                    [compojure]
                    [ring/ring-json]
                    [ring/ring-defaults]

                    ;; boot tasks
                    [boot-environ]
                    [adzerk/boot-test]
                    [adzerk/boot-reload]
                    [degree9/boot-exec]
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
  '[boot.lein :refer [generate]]
  '[degree9.boot-exec :refer [exec]])

(set-env!
  :source-paths #{"test" "test-resources"}
  :resource-paths #{"src" "resources"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""}
  install {:pom (str (get-env :project))}
  push {:pom (str (get-env :project))}
  )

(deftask start-es
         []
         (exec :process "docker"
               :arguments ["run"
                           "-p" "9200:9200"
                           "-p" "9300:9300"
                           "-e" "discovery.type=single-node"
                           "-e" "xpack.security.enabled=false"
                           "docker.elastic.co/elasticsearch/elasticsearch:5.6.1"]))

(deftask build []
         (comp
           (pom)
           (sift :include #{#".*_test\.clj"
                            #"log4j.properties"}
                 :invert true)
           (jar)))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
           (build)
           (install)
           (target)))

(deftask mvn-deploy
         "deploy project"
         []
         (comp
           (mvn-build)
           (push :repo "sixsq")))
