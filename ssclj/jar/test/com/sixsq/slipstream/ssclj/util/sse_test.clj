(ns com.sixsq.slipstream.ssclj.util.sse-test
  (:require [clojure.test :refer [deftest is]]
            [ring.mock.request :as mock]
            [com.sixsq.slipstream.ssclj.util.sse :as sse]))

(deftest sse-start-stream
  (let [request (-> (mock/request :get "/events"))
        handler (sse/event-channel-handler (fn [request response raise ch] ch))
        response @(handler request)
        {body                                          :body
         {content-type  "Content-Type"
          connection    "Connection"
          cache-control "Cache-Control"
          X-Accel-Buffering "X-Accel-Buffering"
          allow-origin  "Access-Control-Allow-Origin"} :headers
         status                                        :status} response]
    (is body "Response has a body")
    (is (instance? manifold.stream.async.CoreAsyncSource body) "Response body is a channel.")
    (is (= 200 status) "A successful status code is sent to the client.")
    (is (= "text/event-stream; charset=UTF-8" content-type)
        "The mime type and character encoding are set with the servlet setContentType method.")
    (is (= "close" connection) "The client is instructed to close the connection.")
    (is (= "no-cache" cache-control) "The client is instructed not to cache the event stream.")
    (is (= "close" connection) "The client is instructed to close the connection.")
    (is (= "no" X-Accel-Buffering) "The server is instructed to allow unbuffered response.")))

