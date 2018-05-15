(ns com.sixsq.slipstream.tools.cli.dbcopy
  (:require
    [clojure.core.async :as async]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [com.sixsq.slipstream.tools.cli.utils :as u]
    [qbits.spandex :as sdx]
    [taoensso.timbre :as log])
  (:gen-class))


(def valid-logging-levels #{:trace :debug :info :warn :error :fatal :report})


(defn db-client
  [host port]
  (let [host-port (str host ":" port)]
    (sdx/client {:hosts [host-port]})))


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
  [index type page]
  (let [action {:index {:_index index, :_type type}}]
    (->> page
         :body
         :hits
         :hits
         (map :_source)
         (map (partial complete-index-action action)))))


(defn bulk-insert
  "Start the bulk insert for the provided actions/documents. A channel which
   will hold the results is returned."
  [client actions]
  (let [{:keys [input-ch output-ch]} (sdx/bulk-chan client {:flush-threshold         100
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



(defn copy-resources
  [{:keys [src-host src-port src-index src-type
           dest-host dest-port dest-index dest-type] :as options}]
  (async/go
    (with-open [src (db-client src-host src-port)
                dest (db-client dest-host dest-port)]

      (let [src-url [src-index src-type :_search]
            query-all {:size  1000
                       :query {:match_all {}}}
            ch (sdx/scroll-chan src {:url src-url, :body query-all})]

        (log/info "starting copy of resources from " src-url)
        (let [[total freq] (loop [stats [0 {}]]
                             (log/infof "processing next page; current stats %s" (str stats))
                             (if-let [page (async/<! ch)]
                               (let [resp-stats (if (instance? Throwable page)
                                                  (do
                                                    (log/error "scroll result exception: " page)
                                                    [0 {}])
                                                  (->> page
                                                       (create-actions dest-index dest-type)
                                                       (bulk-insert dest)
                                                       handle-results))]
                                 (recur (merge-stats stats resp-stats)))
                               stats))]
          (let [treated (reduce + (vals freq))
                created (get freq 201 0)
                stats [total treated created]
                msg (format "finished copy documents from %s - %s" src-url (str stats))]
            (if (apply not= stats)
              (log/error msg)
              (log/info msg))
            stats))))))

;;
;; Command line options processing.
;;

(def cli-options
  [[nil "--src-host HOST" "source ES database host name"
    :default "localhost"]
   [nil "--src-port PORT" "source ES database port"
    :default "9200"]
   [nil "--src-index INDEX" "source ES database index"
    :default "resources-index"]
   [nil "--src-type TYPE" "source ES database type"]

   [nil "--dest-host HOST" "destination ES database host name"]
   [nil "--dest-port PORT" "destination ES database port"
    :default "9200"]
   [nil "--dest-index INDEX" "destination ES database index"]
   [nil "--dest-type TYPE" "destination ES database type"
    :default "_doc"]

   ["-h" "--help"]
   ["-l" "--logging LEVEL" "Logging level: trace, debug, info, warn, error, fatal, or report."
    :id :level
    :default :info
    :parse-fn #(-> % str/lower-case keyword)]])


(def prog-help
  "
  Copies documents from a source Elasticsearch database to a destination
  Elasticsearch database.

  You must specify the server, index, and type for both the source and
  destination.")


(defn usage
  [options-summary]
  (str/join \newline
            [""
             "Initializes a persistent database for SlipStream resources."
             ""
             "Usage: [options]"
             ""
             "Options:"
             options-summary
             ""
             prog-help]))


(defn -main
  [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]

    (log/set-level! (or (valid-logging-levels (:level options)) :info))

    (log/debug "parsed command line options:\n" (with-out-str (pprint options)))

    (cond
      (:help options) (u/success (usage summary))
      errors (u/failure (u/error-msg errors)))

    (let [results (async/<!! (copy-resources options))]
      (if (apply not= results)
        (u/failure "errors occurred while copying documents")
        (u/success)))))
