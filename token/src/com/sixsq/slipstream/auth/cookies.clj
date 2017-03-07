(ns com.sixsq.slipstream.auth.cookies
  "utilities for embedding and extracting tokens in cookies"
  (:require
    [com.sixsq.slipstream.auth.utils.timestamp :as t]
    [com.sixsq.slipstream.auth.sign :as sg]))

(defn revoked-cookie
  "Returns a cookie (as a map) that expires immediately and has 'INVALID' as
   the value. Useful for revoking a cookie in the client's cache. If a name
   is provided, then a map is returned with a single entry with the key being
   the name and the value being the cookie."
  ([]
   {:value "INVALID", :path "/", :max-age 0, :expires (t/formatted-expiry-now)})
  ([name]
    {name (revoked-cookie)}))

(defn claims-cookie
  "Return a cookie (as a map) that has a token generate from the provided
   claims and the default expiration time. If a name is provided, then a map
   is returned with a single entry with the key being the name and the value
   being the cookie."
  ([claims]
   (let [timestamp (t/expiry-later)
         claims (assoc claims :exp timestamp)
         token (sg/sign-claims claims)]
     {:value token, :secure true, :path "/", :expires (t/format-timestamp timestamp)}))
  ([claims name]
    {name (claims-cookie claims)}))
