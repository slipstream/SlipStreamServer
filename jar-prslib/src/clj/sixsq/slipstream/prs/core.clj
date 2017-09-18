(ns sixsq.slipstream.prs.core
  "
  # Library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.data.json :as json]
    [sixsq.slipstream.client.impl.utils.http-sync :as http]
    [clojure.tools.logging :as log]))

(defn call-prs
  [body endpoint & [http-params]]
  (:body (http/put endpoint (merge {:body body} http-params))))

(defn prs-place-and-rank
  [endpoint prs-request]
  (if (or (empty? prs-request) (some empty? (vals prs-request)))
    {:components []}
    (-> (json/write-str prs-request)
        (call-prs endpoint {:accept       "application/json"
                            :content-type "application/json"})
        (json/read-str :key-fn keyword))))

(defn build-prs-input
  [input]
  (select-keys input [:user-connectors :placement-params :components]))

(defn place-and-rank
  "Given the input map, calls PRS service and retuns the JSON returned by PRS."
  [input]
  (log/info "place-and-rank, input = " input)
  (json/write-str
    (if (empty? input)
      {}
      (prs-place-and-rank (:prs-endpoint input) (build-prs-input input)))))
