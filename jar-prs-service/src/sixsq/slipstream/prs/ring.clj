(ns sixsq.slipstream.prs.ring
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clojure.walk :refer [keywordize-keys]]
    [compojure.core :refer [defroutes PUT]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-params]]
    [sixsq.slipstream.placement.core :as pc]
    [com.sixsq.slipstream.auth.cookies :as cookies]))

(defn- authenticated?
  [request]
  (let [cookie (get-in request [:cookies "com.sixsq.slipstream.cookie"])]
    (cookies/extract-cookie-info cookie)))

(defn- place-and-rank
  [params]
  (log/debug "PRS request: " params)
  (let [prs-req  (clojure.walk/keywordize-keys params)
        prs-resp (pc/place-and-rank prs-req)]
    (log/debug "PRS response: " prs-resp)
    {:status 200
     :body   (json/write-str prs-resp)}))

(defn- status-401
  [msg]
  {:status 401
   :body   (json/write-str {:status 401 :message msg})})

(defn- put-place-and-rank
  [request]
  (if (authenticated? request)
    (place-and-rank (:params request))
    (status-401 "PRS service requres access with a valid token in the cookie.")))

(defroutes app-routes
           (wrap-json-params
             (PUT "/filter-rank" request (put-place-and-rank request)))

           (route/not-found {:status 404 :body "Not found"}))

(def handler
  (wrap-defaults
    app-routes
    (assoc-in site-defaults [:security :anti-forgery] false)))

(defn init []
  [handler nil])
