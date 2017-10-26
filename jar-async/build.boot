(def +version+ "3.39-SNAPSHOT")

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

                   [adzerk/boot-reload]
                   [tolitius/boot-check]
                   [boot-codox]
                   [onetom/boot-lein-generate]]))))

(require
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]]
  '[codox.boot :refer [codox]]
  '[boot.lein :refer [generate]])

(set-env!
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)})


