(ns com.sixsq.slipstream.auth.machine
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.data.json :as json]

    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.auth.utils.http :as uh]))

(defn create-token
  [claims]
  (try
    (sign/sign-claims claims)
    (catch Exception e
      (log/error "error signing machine token claims:" (str e))
      nil)))

(defn valid-token?
  [token]
  (try
    (sign/unsign-claims token)
    true
    (catch Exception e
      (log/error "invalid authn token when creating machine token:" (str e))
      false)))

(defn read-json [s]
  (try
    (json/read-str s :key-fn keyword)
    (catch Exception e
      (log/error "invalid json for machine token claims:" (str e))
      nil)))

(defn machine-token
  "signs the claims for a machine token if the given user authentication token is valid"
  [{{:keys [claims token]} :params :as request}]
  (if (valid-token? token)
    (if-let [claims-token (some-> claims
                                  read-json
                                  create-token)]
      (uh/response-with-body 200 (:token claims-token))
      (uh/response 400))
    (uh/response-forbidden)))
