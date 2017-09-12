(ns com.sixsq.slipstream.ssclj.globalstate.inserter-impl
  (:require
    [clj-time.core :as t]
    [qbits.spandex :as spandex]
    [clojure.core.async :as async])
  (:gen-class)
  )


;;TODO : parse args for the actual ES endpoint
(def ^:const client (spandex/client {:hosts ["http://127.0.0.1:9200"]}))

;;define the target index for the global state
(def ^:const index-action {:index {:_index "global-state" :_type "snapshot"}})


(defn bulk-insert
  [snapshots]
  (let [{:keys [input-ch output-ch]} (spandex/bulk-chan (eval 'client) {:flush-threshold         100
                                                                        :flush-interval          5000
                                                                        :max-concurrent-requests 3})
        bulk-instructions (vec (interleave (repeat index-action) snapshots))]

    (async/put! input-ch bulk-instructions)))


(defn use-pagination
  [page timestamp]
  (let [read-vms (-> page
                  :body
                  :hits
                  :hits)
        source-vms (map :_source read-vms)
        snapshots (map #(assoc % :snapshot-time timestamp) source-vms)]
    (bulk-insert snapshots)
    )
  )


(defn fetch-global-state
  "Retrieves all virtual-machine records ."
  [& args]
  (async/go
    (let [
          timestamp (str (t/now)) ;; common timestamp for all snapshots
          ch (spandex/scroll-chan (eval 'client) {:url "resources-index/virtual-machine/_search" :body {:query {:match_all {}}}})]
      (loop []
        (when-let [page (async/<! ch)]
          (use-pagination page timestamp)
          (recur))))))

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]
  (apply fetch-global-state args))
