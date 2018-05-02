(ns com.sixsq.slipstream.ssclj.resources.spec.schema.util
  (:require [qbits.spandex :as spandex]))


(defn create-index [index mapping]
  "Create an index with provided properties map"
  (with-open [client (spandex/client {:hosts ["http://127.0.0.1:9200"]})]
    (try
      (spandex/request client
                 {:url    [index]
                  :method :put
                  :body mapping}
                 )
      (catch Exception e
        (clojure.pprint/pprint e)))))

(defn remove-index [index]
  (with-open [client (spandex/client {:hosts ["http://127.0.0.1:9200"]})]

    (try
      (spandex/request client
                 {:url    [index]
                  :method :delete})
      (catch Exception e
        (clojure.pprint/pprint e)
        ))))