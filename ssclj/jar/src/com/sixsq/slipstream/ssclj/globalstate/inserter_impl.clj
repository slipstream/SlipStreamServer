(ns com.sixsq.slipstream.ssclj.globalstate.inserter-impl
  (:require
    [clj-time.core :as t]
    [qbits.spandex :as spandex]
    [clojure.core.async :as async]
    [com.sixsq.slipstream.ssclj.globalstate.scheduler :as scheduler])
  (:gen-class)
  )


;;define the target index for the global state
(def ^:const index-action {:index {:_index "global-state" :_type "snapshot"}})
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
    (bulk-insert client snapshots)
    )
  )


(defn fetch-global-state
  "Retrieves all virtual-machine records ."
  []
  (async/go
    (let [
          timestamp (str (t/now)) ;; common timestamp for all snapshots
          client (spandex/client {:hosts ["http://127.0.0.1:9200"]})
          ch (spandex/scroll-chan client
                                  {:url search-all-virtual-machines-url :body {:query {:match_all {}}}})]
      (loop []
        (when-let [page (async/<! ch)]
          (use-pagination client page timestamp)
          (recur))))))


(defn start []
  (scheduler/periodically #(fetch-global-state) immediately every-minute))


(defn stop []
  (scheduler/shutdown))

(defn -main [& args]
  (start))


