(ns sixsq.slipstream.metering.metering
  "Core functions that copy a set of resource documents into 'metering'
   documents."
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [clojure.core.async :as async]
    [clj-time.core :as time]
    [qbits.spandex :as spandex]
    [sixsq.slipstream.metering.utils :as utils]))

(def ^:const metering-resource-uri "http://sixsq.com/slipstream/1/Metering")

;; https://github.com/SixSq/SlipStreamPricing/blob/master/Schema.md
;; per Year = ANN, per Month = MON, per Week = WEE, per Day = DAY, per Hour = HUR, per Minute = MIN, per Second = SEC.
(def ^:const price-divisor {"SEC" (/ 1. 60), "MIN" 1, "HUR" 60, "DAY" (* 60 24), "WEE" (* 60 24 7)})

(defn es-hosts
  [host port]
  [(format "http://%s:%s" host port)])


(defn index-action [index type]
  {:index {:_index index, :_type type}})


(defn search-url [index type]
  (str/join "/" [index type "_search"]))


(defn process-options
  [{:keys [es-host es-port
           resources-index resources-type
           metering-index metering-type
           metering-period-minutes]
    :or   {es-host                 "127.0.0.1"
           es-port                 9200
           resources-index         "resources-index"
           resources-type          "virtual-machine"
           metering-index          "resources-index"
           metering-type           "metering"
           metering-period-minutes 1}}]
  {:hosts                   (es-hosts es-host es-port)
   :resource-search-url     (search-url resources-index resources-type)
   :metering-action         (index-action metering-index metering-type)
   :metering-period-minutes metering-period-minutes})


(defn assoc-snapshot-time
  [timestamp m]
  (assoc m :snapshot-time timestamp))

;; TODO: quantization for hour period, i.e apply the full hour price to first minute then zero for the rest of the hour
(defn assoc-price
  [{:keys [serviceOffer] :as m}]
  (let [price-map (when (:price:unitCost serviceOffer)
                    (some->> serviceOffer
                             :price:unitCode
                             (get price-divisor)
                             (/ (:price:unitCost serviceOffer))
                             (assoc {} :price)))]
    (merge m price-map)))

(defn update-id
  [timestamp {:keys [id] :as m}]
  (let [uuid (second (str/split (or id (utils/random-uuid)) #"/"))
        ts (str/replace timestamp #"[:\.]" "-")
        new-id (str "metering/" uuid "-" ts)]
    (assoc m :id new-id)))


(defn replace-resource-uri
  [m]
  (assoc m :resourceURI metering-resource-uri))


(defn create-actions
  "work on a subset of documents returned by the global query search"
  [timestamp index-action page]
  (->> page
       :body
       :hits
       :hits
       (map :_source)
       (map (partial assoc-snapshot-time timestamp))
       (map assoc-price)
       (map (partial update-id timestamp))
       (map replace-resource-uri)
       (map (fn [v] [index-action v]))))


(defn bulk-insert
  "Start the bulk insert for the provided actions/documents. A channel which
   will hold the results is returned."
  [client actions]
  (let [{:keys [input-ch output-ch]} (spandex/bulk-chan client {:flush-threshold         100
                                                                :flush-interval          1000
                                                                :max-concurrent-requests 3})]
    (when (pos? (count actions))
      (doseq [action actions]
        (async/put! input-ch action)))
    (async/close! input-ch)
    output-ch))


(defn response-stats
  [resp]
  (if (instance? Throwable resp)
    (do
      (log/error resp)
      [0 {}])
    (let [[job responses] resp
          n (count job)
          freq (frequencies (->> responses
                                 :body
                                 :items
                                 (map :index)
                                 (map :status)))]
      [n freq])))


(defn merge-stats
  [& stats]
  [(reduce + 0 (map first stats))
   (or (apply merge-with + (map second stats)) {})])


(defn handle-results
  [ch]
  (let [results (loop [stats [0 {}]]
                  (if-let [resp (async/<!! ch)]
                    (let [resp-stats (response-stats resp)]
                      (recur (merge-stats stats resp-stats)))
                    stats))]
    (log/debug "bulk insert stats:" results)
    results))


(defn meter-resources
  [hosts resource-search-url metering-action]
  (async/go
    (with-open [client (spandex/client {:hosts hosts})]
      (let [timestamp (str (time/now))
            ch (spandex/scroll-chan client
                                    {:url  resource-search-url
                                     :body {:query {:match_all {}}}})]

        (log/info "start metering snapshot" timestamp "from" resource-search-url)
        (let [[total freq] (loop [stats [0 {}]]
                             (if-let [page (async/<! ch)]
                               (let [resp-stats (if (instance? Throwable page)
                                                  (do
                                                    (log/error "scroll result exception: " page)
                                                    [0 {}])
                                                  (->> page
                                                       (create-actions timestamp metering-action)
                                                       (bulk-insert client)
                                                       handle-results))]
                                 (recur (merge-stats stats resp-stats)))
                               stats))]
          (let [treated (reduce + (vals freq))
                created (get freq 201 0)
                stats [total treated created]
                msg (str "finish metering snapshot " timestamp
                         " from " resource-search-url
                         " - " stats)]
            (if (apply not= stats)
              (log/error msg)
              (log/info msg))
            stats))))))
