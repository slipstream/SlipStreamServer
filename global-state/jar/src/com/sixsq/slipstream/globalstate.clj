(ns com.sixsq.slipstream.globalstate
  "SlipStream  global state project."
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [qbits.spandex :as spandex]
    [clojure.core.async :as async]
    [environ.core :as env]
    [com.sixsq.slipstream.scheduler :as scheduler]))


;;define the target index for the global state
(def ^:const index-action {:index {:_index "resources-index" :_type "global-state-snapshot"}})
;;initial delay
(def ^:const immediately 0)
;;task interval
(def ^:const every-minute (* 60 1000))
;;global ES query to search every virtual-machine documents
(def ^:const search-all-virtual-machines-url "resources-index/virtual-machine/_search")

(defn bulk-insert
  "Translate the snapshots (current global state) into ES bulk instructions"
  [client snapshots]
  (let [{:keys [input-ch output-ch]} (spandex/bulk-chan client {:flush-threshold         100
                                                                :flush-interval          1000
                                                                :max-concurrent-requests 3})
        bulk-instructions (vec (interleave (repeat index-action) snapshots))]
    (log/debug "handling bulk insert instructions for " (count snapshots) " resources")
    (async/put! input-ch bulk-instructions)))


(defn use-pagination
  "work on a subset of documents returned by the global query search"
  [client page timestamp]
  (let [read-vms (-> page
                     :body
                     :hits
                     :hits)
        source-vms (map :_source read-vms)
        snapshots (map #(assoc % :snapshot-time timestamp) source-vms)]
    (bulk-insert client snapshots)))


(defn fetch-global-state
  "Retrieves all virtual-machine records ."
  []
  (async/go
    (let [
          timestamp (str (t/now))                           ;; common timestamp for all snapshots
          es-host (or (env/env :es-host) "127.0.0.1")
          es-port (or (env/env :es-port-http) 9200)
          client (spandex/client {:hosts [(str "http://" es-host ":" es-port)]})
          ch (spandex/scroll-chan client
                                  {:url search-all-virtual-machines-url :body {:query {:match_all {}}}})]
      (log/info "global state snapshots at " timestamp)
      (loop []
        (when-let [page (async/<! ch)]
          (use-pagination client page timestamp)
          (recur)))
      )
    :finished ;;don't return nil
    ))

(defn handler [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Global State service is running!"})

(defn cleanup []
  (log/debug "Shutting down scheduler")
  (scheduler/shutdown))

(defn init []
  (log/debug "Starting global state insertions")
  (scheduler/periodically #(async/<!! (fetch-global-state)) immediately every-minute)
  [handler cleanup]
  )






