(ns com.sixsq.slipstream.auth.cookies
  "utilities for embedding and extracting tokens in cookies"
  (:require
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defn revoked-cookie
  "Returns a cookie (as a map) that expires immediately and has 'INVALID' as
   the value. Useful for revoking a cookie in the client's cache. If a name
   is provided, then a map is returned with a single entry with the key being
   the name and the value being the cookie."
  ([]
   {:value "INVALID", :path "/", :max-age 0, :expires (ts/formatted-expiry-now)})
  ([name]
   {name (revoked-cookie)}))

(defn claims-cookie
  "Return a cookie (as a map) that has a token generated from the provided
   claims and the default expiration time. If a name is provided, then a map
   is returned with a single entry with the key being the name and the value
   being the cookie."
  ([claims]
   (let [timestamp (ts/expiry-later)
         claims (assoc claims :exp timestamp)
         token (sg/sign-claims claims)]
     {:value   {:token token}                               ;; FIXME: Remove :token, required java-side changes as well.
      :secure  true
      :path    "/"
      :expires (ts/format-timestamp timestamp)}))
  ([claims name]
   {name (claims-cookie claims)}))

(defn extract-claims
  "Extracts the claims from the value of a cookie. Throws an exception if the
   claims are not valid or cannot be extracted. NOTE: This can only be used on
   cookies that have been serialized through the ring wrap-cookies middleware!
   You cannot roundtrip the claims directly through `claims-cookie` and
   `extract-claims`."
  [{:keys [value] :as cookie}]
  (when value
    (-> value
        (str/split #"^token=")                              ;; FIXME: remove this
        second                                              ;; FIXME: and this
        sg/unsign-claims)))

(defn claims->authn-info
  "Returns a tuple with the username (identifier) and list of roles based on the
   provided claims map."
  [claims]                                                  ;; FIXME: Normalize the keyword names.
  (when-let [identifier (get claims :com.sixsq.identifier)]
    (let [session (get claims :com.sixsq.session)
          roles (remove str/blank? (-> (get claims :com.sixsq.roles)
                                       (or "")
                                       (str/split #"\s+")))
          roles (if session
                  (conj roles session)
                  roles)]
      [identifier roles])))                                 ;; FIXME: Returned roles should really be a set.

(defn extract-cookie-claims
  "Extracts authentication claims from a cookie. Returns nil if no cookie is
   provided or if there is an error when extracting the claims from the cookie."
  [cookie]
  (try
    (-> cookie
        extract-claims)
    (catch Exception e
      (log/warn "Error in extract-cookie-claims: " (str e))
      nil)))

(defn extract-cookie-info
  "Extracts authentication information from a cookie. Returns nil if no cookie is
   provided or if there is an error when extracting the claims from the cookie."
  [cookie]
  (-> cookie
      extract-cookie-claims
      claims->authn-info))

