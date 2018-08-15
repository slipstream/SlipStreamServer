(ns sixsq.slipstream.metering.metering
  "Core functions that copy a set of resource documents into 'metering'
   documents."
  (:require
    [clj-time.core :as time]
    [clojure.core.async :as async]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [qbits.spandex :as spandex]
    [sixsq.slipstream.metering.utils :as utils]))

(def ^:const metering-resource-uri "http://sixsq.com/slipstream/1/Metering")

;; https://github.com/SixSq/SlipStreamPricing/blob/master/Schema.md
;; per Year = ANN, per Month = MON, per Week = WEE, per Day = DAY, per Hour = HUR, per Minute = MIN, per Second = SEC.
(def ^:const price-divisor {"SEC" (/ 1. 60), "MIN" 1, "HUR" 60, "GiBh" 60, "MiBh" 60,"DAY" (* 60 24), "WEE" (* 60 24 7)})

(def ^:const quantity-divisor {"GiBh" (* 1024 1024), "MiBh" 1024 })

(def ^:const doc-type "_doc")

(defn es-hosts
  [host port]
  [(format "http://%s:%s" host port)])


(defn index-action [index type]
  {:index {:_index index, :_type type}})

(defn search-url [index type]
  (str/join "/" [index type "_search"]))

(defn search-urls [indices types]
  (map #(search-url %1 %2) indices types))

(defn process-options
  [{:keys [es-host es-port
           vm-index
           bucky-index
           metering-index
           metering-period-minutes]
    :or   {es-host                 "127.0.0.1"
           es-port                 9200
           vm-index                "slipstream-virtual-machine"
           bucky-index             "slipstream-storage-bucket"
           metering-index          "slipstream-metering"
           metering-period-minutes 1}}]
  {:hosts                   (es-hosts es-host es-port)
   :resource-search-urls    (search-urls [vm-index bucky-index] [doc-type doc-type])
   :metering-action         (index-action metering-index doc-type)
   :metering-period-minutes metering-period-minutes})

(defn assoc-snapshot-time
  [timestamp m]
  (assoc m :snapshot-time timestamp))

(defn quantity
  [{:keys [usageInKiB] :as resource}]
  (let [billingUnit (when usageInKiB (-> resource
                                    :serviceOffer
                                    :price:billingUnit))]
  (if usageInKiB (/ usageInKiB (get quantity-divisor billingUnit (* 1024 1024))) 1)))

(defn add-unitCode
  [{:keys [price:unitCode] :as serviceOffer}]
  (if price:unitCode
    serviceOffer
    (assoc serviceOffer
      :price:unitCode
      (or (:price:billingUnit serviceOffer)
          (:price:billingUnitCode serviceOffer)))))

;; TODO: quantization for hour period, i.e apply the full hour price to first minute then zero for the rest of the hour
(defn assoc-price
  [{:keys [serviceOffer] :as m}]
  (let [so (when (and serviceOffer (map? serviceOffer))(add-unitCode serviceOffer))
        price-map (when (:price:unitCost so)
                    (some->> so
                             :price:unitCode
                             (get price-divisor)
                             (/ (:price:unitCost serviceOffer))
                             (* (quantity m))
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


(defn complete-index-action
  "Add the :_id key to the index action so that the Elasticsearch :_id key is
   consistent with the CIMI resourceID. The :_type key should already be
   present in the index-action parameter."
  [index-action {:keys [id] :as v}]
  (let [action (first (keys index-action))
        args (first (vals index-action))
        uuid (second (str/split id #"/"))]
    [{action (assoc args :_id uuid)} v]))


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
       (map (partial complete-index-action index-action))))


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

(defn- meter-resource
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

(defn meter-resources
  [hosts resource-search-urls metering-action]
  (doall (map #(meter-resource hosts % metering-action) resource-search-urls)))
