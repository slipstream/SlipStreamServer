(ns sixsq.slipstream.placement.cimi-util
  (:require
    [environ.core :as e]
    [clojure.tools.logging :as log]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.api.cimi.sync :as sync]))

(defn- mandatory-env-value
  [env-var-name]
  (if-let [value (e/env env-var-name)]
    value
    (throw (Exception. (str "Environment variable '" env-var-name "' not defined.")))))

(def cached-cimi-context (atom {:context nil :timestamp -1}))

(def token-obsolete-millis (delay (* 60 1000 (read-string (mandatory-env-value :ss-token-obsolete-mn)))))
(def cimi-cloud-entry-point (delay (mandatory-env-value :ss-cimi-cloud-entry-point)))
(def cimi-username (delay (mandatory-env-value :ss-cimi-username)))
(def cimi-password (delay (mandatory-env-value :ss-cimi-password)))
(def url-login (delay (mandatory-env-value :ss-url-login)))
(def url-logout (delay (mandatory-env-value :ss-url-logout)))

(defn- timestamp-token-obsolete?
  [timestamp]
  (> (- (System/currentTimeMillis) timestamp) @token-obsolete-millis))

(defn update-context
  []
  (log/info "updating cimi server token for user" @cimi-username)
  (let [context (sync/instance @cimi-cloud-entry-point @url-login @url-logout)]
    (cimi/login context {:username @cimi-username
                         :password @cimi-password})
    (reset! cached-cimi-context {:context   context
                                 :timestamp (System/currentTimeMillis)})))

(defn- update-context-when-obsolete
  []
  (when (timestamp-token-obsolete? (:timestamp @cached-cimi-context))
    (update-context)))

(defn context
  []
  (update-context-when-obsolete)
  (:context @cached-cimi-context))
