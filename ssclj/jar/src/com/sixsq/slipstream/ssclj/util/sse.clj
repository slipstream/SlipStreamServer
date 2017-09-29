(ns com.sixsq.slipstream.ssclj.util.sse
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [ring.core.protocols :as ring]
            [ring.util.response :as ring-response]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

(def CRLF "\r\n")
(def EVENT_FIELD "event: ")
(def DATA_FIELD "data: ")
(def ID_FIELD "id: ")

(defn mk-data
  ([name data]
   (mk-data name data nil))
  ([name data id]
   (let [sb (StringBuilder.)]
     (when name
       (.append sb EVENT_FIELD)
       (.append sb name)
       (.append sb CRLF))

     (doseq [part (string/split data #"\r?\n")]
       (.append sb DATA_FIELD)
       (.append sb part)
       (.append sb CRLF))

     (when (not-empty id)
       (.append sb ID_FIELD)
       (.append sb id)
       (.append sb CRLF))

     (.append sb CRLF)
     (str sb))))

(defn send-event [id name data event-ch]
  (let [event {:id   id
               :name name
               :data data}]
    (async/>!! event-ch event)))

(defn- send-event-on-response-ch
  [channel name data id put-fn raise]
  (try
    (put-fn channel (mk-data name data id))
    (catch Throwable t
      (async/close! channel)
      (raise t)
      nil)))

(defn- start-dispatch-loop
  "Kicks off the loop that transfers data provided by the application
  on `event-channel` to the HTTP infrastructure via
  `response-channel`."
  [{:keys [event-channel response-channel heartbeat-delay on-client-disconnect raise] :as opts}]
  (async/go
    (loop []
      (let [hb-timeout (async/timeout (* 1000 heartbeat-delay))
            [event port] (async/alts! [event-channel hb-timeout])]
        (cond
          (= port hb-timeout)
          (when (async/>! response-channel CRLF)
            (recur))

          (and (some? event) (= port event-channel))
          (let [{event-name :name
                 event-data :data
                 event-id   :id}
                (if (map? event)
                  (reduce (fn [agg [k v]] (assoc agg k (str v))) {} event)
                  {:data (str event)})]
            (when (send-event-on-response-ch response-channel event-name event-data event-id async/put! raise)
              (recur))))))
    (async/close! event-channel)
    (async/close! response-channel)
    (when on-client-disconnect (on-client-disconnect))
    :done))

(defn- start-stream
  "Starts an SSE event stream and initiates a heartbeat to keep the
  connection alive. `stream-ready-fn` will be called with a core.async
  channel and the initial response map. The application can then put
  maps with keys :id, :name, and :data on that channel to cause SSE
  events to be sent to the client. Either the client or the
  application may close the channel to terminate and clean up the
  event stream; the client closes it by closing the connection.  The
  SSE's core.async buffer can either be a fixed buffer (n) or a
  0-arity function that returns a buffer."
  [{:keys [stream-ready-fn request respond raise heartbeat-delay bufferfn-or-n on-client-disconnect]}]
  (let [heartbeat-delay (or heartbeat-delay 10)
        bufferfn-or-n (or bufferfn-or-n 10)
        response-channel (async/chan (if (fn? bufferfn-or-n) (bufferfn-or-n) bufferfn-or-n))
        response (-> (ring-response/response (s/->source response-channel))
                     (ring-response/content-type "text/event-stream") ;; TODO: content negotiation? "text/event-stream+json"?
                     (ring-response/charset "UTF-8")
                     (ring-response/header "Connection" "close")
                     (ring-response/header "Cache-Control" "no-cache")
                     (ring-response/header "X-Accel-Buffering" "no")) ; X-Accel-Buffering needed to disable nginx buffering
        ;; TODO: re-create CORS support as per original: (update-in [:headers] merge (:cors-headers context))
        event-channel (async/chan (if (fn? bufferfn-or-n) (bufferfn-or-n) bufferfn-or-n))]
    (respond response)
    (async/thread
      (stream-ready-fn request response raise event-channel)
      :done)
    (start-dispatch-loop (merge {:event-channel    event-channel
                                 :response-channel response-channel
                                 :heartbeat-delay  heartbeat-delay
                                 :raise            raise}
                                (when on-client-disconnect
                                  {:on-client-disconnect #(on-client-disconnect response)})))))

(defn ring->aleph [handler]
  (fn [request]
    (let [response (d/deferred)]
      (handler request #(d/success! response %) #(d/error! response %))
      response)))

(defn event-channel-handler
  "Returns a Ring async handler which will start a Server Sent Event
  stream with the requesting client. `stream-ready-fn` will be called
  in a future, and will be passed the original request, the initial
  response, the raise fn, and the event channel.

  Options:

  :buffer - either an integer buffer size, or a 0-arity function that
    returns a buffer.
  :heartbeat-delay - An integer number of seconds between heartbeat
    messages
  :on-client-disconnect - A function of one argument (the initial response)
    which will be called when the client permanently disconnects."
  ([stream-ready-fn]
   (event-channel-handler stream-ready-fn {}))
  ([stream-ready-fn {:keys [buffer heartbeat-delay on-client-disconnect]}]
   (ring->aleph
     (fn [request respond raise]
       (start-stream {:stream-ready-fn      stream-ready-fn
                      :request              request
                      :respond              respond
                      :raise                raise
                      :heartbeat-delay      heartbeat-delay
                      :bufferfn-or-n        buffer
                      :on-client-disconnect on-client-disconnect})))))
