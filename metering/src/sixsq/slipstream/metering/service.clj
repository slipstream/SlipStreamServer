(ns sixsq.slipstream.metering.service
  (:require
    [clojure.core.async :as async]
    [clojure.tools.logging :as log]
    [environ.core :as env]
    [sixsq.slipstream.metering.metering :as metering]
    [sixsq.slipstream.metering.scheduler :as scheduler]
    [sixsq.slipstream.metering.utils :as utils]))


(defn meter-resources-sync [hosts resource-search-urls metering-action]
  (map async/<!! (metering/meter-resources hosts resource-search-urls metering-action)))


(defn handler [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Metering service is running!"})


(defn cleanup []
  (log/debug "Shutting down scheduler")
  (scheduler/shutdown))


(defn init []
  (let [{:keys [hosts
                resource-search-urls
                metering-action
                metering-period-minutes]}
        (metering/process-options env/env)]
    (log/info "starting service"
              "\nhosts:" hosts
              "\nresource-search-urls: " resource-search-urls
              "\nmetering-action:" metering-action)
    (scheduler/periodically #(meter-resources-sync hosts
                                                   resource-search-urls
                                                   metering-action)
                            (utils/str->int metering-period-minutes)))
  [handler cleanup])






