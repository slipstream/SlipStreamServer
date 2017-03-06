(ns sixsq.slipstream.prs.ring
  (:require
    [clojure.data.json :as json]
    [clojure.string :as s]
    [clojure.tools.logging :as log]
    [clojure.walk :refer [keywordize-keys]]
    [compojure.core :refer [defroutes PUT]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-params]]
    [sixsq.slipstream.placement.core :as pc]
    [com.sixsq.slipstream.auth.sign :as sign]))

;; Taken from com.sixsq.slipstream.ssclj.middleware.authn-info-header,
;; which is part of com.sixsq.slipstream/SlipStreamCljResources-jar artefact.
;; SlipStreamCljResources-jar is too big for adding it as the dependency.
;; NB! This requires auth_pubkey.pem on the classpath.
(def ^:const authn-cookie
  "com.sixsq.slipstream.cookie")

(defn- extract-cookie-info
  [request]
  (try
    (if-let [token (get-in request [:cookies authn-cookie :value])]
      (let [claims     (sign/unsign-claims (-> token (s/split #"^token=") second))
            identifier (:com.sixsq.identifier claims)
            roles      (remove s/blank? (-> claims
                                            :com.sixsq.roles
                                            (or "")
                                            (s/split #"\s+")))]
        (when identifier
          [identifier roles])))
    (catch Exception ex
      (log/warn (str "Error in extract-cookie-info: " (.getMessage ex)))
      nil)))
;; end.

(defn- authenticated?
  [request]
  (not (nil? (seq (extract-cookie-info request)))))

(defn- place-and-rank
  [params]
  (let [prs-req  (clojure.walk/keywordize-keys params)
        _        (log/debug "PRS request: " prs-req)
        prs-resp (pc/place-and-rank prs-req)
        _        (log/debug "PRS response: " prs-resp)]
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
