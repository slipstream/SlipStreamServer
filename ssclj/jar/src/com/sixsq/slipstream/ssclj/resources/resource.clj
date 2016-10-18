(ns com.sixsq.slipstream.ssclj.resources.common.resource
  "Provides the protocol for CIMI (or CIMI-like resources).")

(defprotocol resource

  (initialize [_])
  (routes [_])

  (add [_ request])
  (query [_ request])
  (retrieve [_ request])
  (retrieve-by-id [_ id])
  (edit [_ request])
  (do-action [_ request])

  (validate [_ resource])
  (set-operations [_ resource])
  (new-identifier [_ json resource-name])
  (add-acl [_ json request]))
