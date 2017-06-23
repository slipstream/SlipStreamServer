(def +version+ "3.30")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamAsync
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
                 '[[org.clojure/clojure]
                   [org.clojure/core.async]

                   [com.sixsq.slipstream/SlipStreamPersistence]
                   [com.sixsq.slipstream/SlipStreamConnector]

                   [com.sixsq.slipstream/SlipStreamDbSerializers-jar nil :scope "test"]

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
  :source-paths #{"test"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  test {:junit-output-to ""})


