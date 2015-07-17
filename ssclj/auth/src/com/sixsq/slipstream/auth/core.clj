(ns com.sixsq.slipstream.auth.core)

(defprotocol AuthenticationServer

  (add-user!
    [this user]
    "Creates and stores credentials for this user")

  (auth-user
    [this credentials]
    "Checks if credentials are valid.
    If yes, returns the authenticated user.")

  ;; TODO
  ;; revoke-user
  ;; reset-user

  )






