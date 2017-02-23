(ns sixsq.slipstream.prs.ring
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clojure.walk :refer [keywordize-keys]]
    [compojure.core :refer [defroutes PUT]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-params]]
    [sixsq.slipstream.placement.core :as pc]))

(defn- put-place-and-rank
  [request]
  (let [prs-req (clojure.walk/keywordize-keys (:params request))
        _ (log/debug "PRS request: " prs-req)
        prs-resp (pc/place-and-rank prs-req)
        _ (log/debug "PRS response: " prs-resp)]
    {:status 200
     :body   (json/write-str prs-resp)}))

(defroutes app-routes
           (wrap-json-params
             (PUT "/filter-rank" request (put-place-and-rank request)))

           (route/not-found {:status 404 :body "Not found"}))

(def handler
  (-> app-routes
      (wrap-defaults (assoc site-defaults :security (assoc (:security site-defaults) :anti-forgery false)))))

(defn init []
  [handler nil])
