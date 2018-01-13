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
  [ss-token]
  (-> ss-token
      cookies/extract-cookie-info
      seq
      nil?
      not))

(defn- place-and-rank
  [token params]
  (log/debug "PRS request: " params)
  (let [prs-req  (clojure.walk/keywordize-keys params)
        prs-resp (pc/place-and-rank token prs-req)]
    (log/debug "PRS response: " prs-resp)
    {:status 200
     :body   (json/write-str prs-resp)}))

(defn- status-401
  [msg]
  {:status 401
   :body   (json/write-str {:status 401 :message msg})})

(defn- put-place-and-rank
  [request]
  (let [ss-token-name "com.sixsq.slipstream.cookie"
        ss-token (get-in request [:cookies ss-token-name])]
    (if (authenticated? ss-token)
      (place-and-rank (str ss-token-name "=" (:value ss-token)) (:params request))
      (status-401 "PRS service requres access with a valid token in the cookie."))))

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
