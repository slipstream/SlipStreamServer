(ns com.sixsq.slipstream.metering
  "SlipStream  global state project."
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [qbits.spandex :as spandex]
    [clojure.core.async :as async]
    [environ.core :as env]
    [com.sixsq.slipstream.scheduler :as scheduler]
    [com.sixsq.slipstream.utils :as utils]))

(def
  ^{:const true
    :doc   "endpoints for elasticsearch nodes"}
  es-hosts [(format "http://%s:%s"
                    (or (env/env :es-host) "127.0.0.1")
                    (or (env/env :es-port) 9200))])

;;define the target index for the metering
(def ^:const index-action {:index {:_index (or (env/env :metering-es-index) "resources-index")
                                   :_type  "metering-snapshot"}})
;;initial delay
(def ^:const immediately 0)
;;task interval
(def ^:const interval-ms (or (env/env :metering-interval) 60000))

(defn insert-action-xf
  "This function returns a transducer that will extract the results of an
   Elasticsearch query and provide a sequence modified documents wrapped in an
   Elasticsearch insert action."
  [timestamp]
  (comp (map :body)
        (map :hits)
        (map :hits)
        (map first)
        (utils/unwrap)
        (map :_source)
        (map #(assoc % :snapshot-time timestamp))
        (map (fn [v] [index-action v]))))

(defn handle-results
  [ch]
  (loop []
    (log/debug "starting handling of results")
    (when-let [[job responses] (async/<!! ch)]
      (log/debug "got responses: " (with-out-str (clojure.pprint/pprint responses)))
      (recur)))
  ::exit)

(defn fetch-global-state
  "Retrieves all virtual-machine records."
  [search-url]
  (async/go
    (with-open [client (spandex/client {:hosts es-hosts})]
      (let [timestamp (str (t/now))                         ;; common timestamp for all snapshots
            ch (async/chan 100 (insert-action-xf timestamp))
            ch (spandex/scroll-chan client
                                    {:ch   ch
                                     :url  search-url
                                     :body {:query {:match_all {}}}})
            {:keys [output-ch]} (spandex/bulk-chan client {:input-ch                ch
                                                           :flush-threshold         100
                                                           :flush-interval          1000
                                                           :max-concurrent-requests 3})]
        (log/info "starting metering snapshot " timestamp " from " search-url)
        (handle-results output-ch)
        (log/info "finished metering snapshot " timestamp " from " search-url)))
    ::exit                                                  ;; don't return nil
    ))

(defn handler [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Metering service is running!"})

(defn cleanup []
  (log/debug "Shutting down scheduler")
  (scheduler/shutdown))

(defn init []
  (let [search-url (or (env/env :metering-search-url) "resources-index/virtual-machine/_search")]
    (scheduler/periodically #(async/<!! (fetch-global-state search-url))
                            (utils/str->int immediately)
                            (utils/str->int interval-ms))
    [handler cleanup]))
