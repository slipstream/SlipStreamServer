(ns slipstream.credcache.renewal
  "Multimethod for renewing a resource that can expire."
  (:require
    [clojure.tools.logging :as log]))

;;
;; renewable credentials must be renewed periodically to maintain
;; a valid credential; implementation of such credentials must provide
;; an implementation of the 'renew' multimethod
;;

(defmulti renew
          "Renews the credential described in the given map.  This
           method dispatches on the value of the :typeURI key.  Implementations
           of this method, must return the updated resource or nil if the
           renewal did not succeed.  The default implementation logs a warning
           and returns nil."
          :typeURI)

(defmethod renew :default
           [{:keys [typeURI]}]
  (log/warn "cannot renew unknown type of credential:" typeURI)
  nil)


