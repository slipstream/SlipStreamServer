(ns com.sixsq.slipstream.ssclj.resources.session-oidc.utils
  (:require [ring.util.codec :as codec]
            [com.sixsq.slipstream.ssclj.resources.session :as p]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.auth.utils.http :as uh]
            [clojure.tools.logging :as log]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.util.response :as r]
            [clojure.string :as str]
            [com.sixsq.slipstream.ssclj.util.log :as logu]
            [environ.core :as environ]))

(defn prefix
  [realm attr]
  (when (and realm attr)
    (str realm ":" attr)))

(defn extract-roles
  [{:keys [realm roles] :as claims}]
  (if (and realm roles)
    (vec (map (partial prefix realm) roles))
    []))

(defn group-hierarchy
  [group]
  (if-not (str/blank? group)
    (let [terms (remove str/blank? (str/split group #"/"))]
      (doall
        (for [i (range 1 (+ 1 (count terms)))]
          (str "/" (str/join "/" (take i terms))))))
    []))

(defn extract-groups
  [{:keys [realm groups] :as claims}]
  (if (and realm groups)
    (->> groups
         (mapcat group-hierarchy)
         (map (partial prefix realm))
         vec)
    []))

;; exceptions specific to cyclone

(defn throw-no-username-or-email [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "OIDC token is missing name/preferred_name (" username ") or email (" email ")") redirectURI))

(defn throw-no-matched-user [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "Unable to match account to name/preferred_name (" username ") or email (" email ")") redirectURI))


;; general exceptions

(defn throw-bad-client-config [redirectURI]
  (logu/log-error-and-throw-with-redirect 500 "missing client ID, base URL, or public key (:oidc-client-id, :oidc-base-url, :oidc-public-key) for OIDC authentication" redirectURI))

(defn throw-missing-oidc-code [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "OIDC authentication callback request does not contain required code" redirectURI))

(defn throw-no-access-token [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve OIDC access token" redirectURI))

(defn throw-no-subject [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "OIDC token is missing subject (sub) attribute") redirectURI))

(defn throw-invalid-access-code [msg redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "error when processing OIDC access token: " msg) redirectURI))

(defn throw-inactive-user [username redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "account is inactive (" username ")") redirectURI))

;; retrieval of configuration parameters

(defn config-params
  [prefix redirectURI methodKey]
  (let [client-id (environ/env (keyword (str prefix "-client-id-" methodKey)))
        base-url (environ/env (keyword (str prefix "-base-url-" methodKey)))
        public-key (environ/env (keyword (str prefix "-public-key-" methodKey)))]
    (if (and client-id base-url public-key)
      [client-id base-url public-key]
      (throw-bad-client-config redirectURI))))

(def oidc-client-info (partial config-params "oidc"))

(def cyclone-client-info (partial config-params "cyclone"))
