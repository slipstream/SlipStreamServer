(ns slipstream.credcache.renewal
  "Renewable credentials must be renewed periodically to maintain a
   valid credential.  This namespace defines a multimethod for this
   renewal.  Each type of renewable credential must implement this
   method."
  (:require
    [clojure.tools.logging :as log]))

(defmulti renew
          "Renews the credential described in the given map.  This
           method dispatches on the value of the :credential-type key.
           Implementations must return the updated resource or nil if the
           renewal did not succeed.  The default implementation logs a warning
           and returns nil."
          :credential-type)

(defmethod renew :default
           [{:keys [credential-type]}]
  (log/warn "cannot renew unknown type of credential:" credential-type)
  nil)


