#!/usr/bin/env boot

;;
;; Boot related scafolding.
(set-env!
  :version "3.12-SNAPSHOT"
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.8.0"]
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
                    [org.clojure/data.xml]
                    [com.sixsq.slipstream/SlipStreamPersistence]
                    [com.sixsq.slipstream/SlipStreamCljResources-jar]
                    [superstring]
                    [me.raynes/fs]
                    [clj-http]]))))
(set-env!
  :resource-paths #{"src"})
(require '[boot.cli :refer [defclifn]])
;; Boot end.

(require '[com.sixsq.slipstream.db.serializers.service-config-impl :as sci])
(require '[com.sixsq.slipstream.db.serializers.service-config-util :as scu])
(require '[me.raynes.fs :as fs])
(require '[clojure.string :as s])
(require '[clj-http.client :as http])

;;
;; Dynamic vars.
(def ^:dynamic *c-names* #{})
(def ^:dynamic *cfg-path-url* nil)
(def ^:dynamic *creds* nil)

(defn fwrite
  [o fpath]
  (let [f (fs/expand-home fpath)]
    (with-open [^java.io.Writer w (apply clojure.java.io/writer f {})]
      (clojure.pprint/pprint o w))))

(defn ->config-resource
  [url]
  (str url "/configuration"))

(defn conf-xml
  [path-url]
  (if (s/starts-with? path-url "https")
    (-> path-url
        ->config-resource
        (http/get {:follow-redirects false
                   :accept           :xml
                   :basic-auth       *creds*})
        :body)
    (slurp path-url)))

(defn run
  []
  (let [sc (-> *cfg-path-url* conf-xml scu/conf-xml->sc)]
    (doseq [[ckey [vals desc]] (sci/sc->connectors sc *c-names*)]
      (let [cname (name ckey)]
        (println "Saving connector:" cname)
        (fwrite vals (format "connector-%s.edn" cname))
        (fwrite desc (format "connector-%s-desc.edn" cname))))))

(def usage "Usage -x <path or URL> [-s <user:pass>] [-c <connector name>]")

(defn error-usage
  [& msg]
  (println (apply str msg))
  (println usage)
  (System/exit 1))

(defclifn -main
          "Extract and store connector parameters and their description.

          Given SlipStream URL or path to file with configuration XML, extracts
          and stores per connector parameters and their description into
          connector-<instance-name>.edn and connector-<instance-name>-desc.edn
          respectively."
          [c connectors CONNECTORS #{str} "Connector instance names (category). If not provided all connectors will be stored."
           x configxml CONFIGXML str "Path to file or URL starting with https (requries -s parameter). Mandatory."
           s credentials CREDENTIALS str "credentials as user:pass for -x when URL is provided."]
          (if (empty? configxml)
            (error-usage "-x parameter must be provided.")
            (alter-var-root #'*cfg-path-url* (fn [_] configxml)))
          (if (and (s/starts-with? configxml "https") (empty? credentials))
            (error-usage "-s must be provided when -x is URL.")
            (alter-var-root #'*creds* (fn [_] credentials)))
          (alter-var-root #'*c-names* (fn [_] connectors))
          (run)
          (System/exit 0))

