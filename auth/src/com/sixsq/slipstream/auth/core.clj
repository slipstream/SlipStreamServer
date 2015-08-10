(ns com.sixsq.slipstream.auth.core)

(defprotocol AuthenticationServer

  (add-user!
    [this user]
    "Creates and stores credentials for this user")

  (auth-user
    [this credentials]
    "Checks if credentials are valid.
    If yes, returns the authenticated user.")

  (token
    [this credentials]
    "Returns a vector containing the signed token (with a private key),
     valid for a given time.
     If credentials are valid [true, token] is returned.
     Otherwise [false, message] is returned.")

  (check-token
    [this token]
    "Verifies that the signed (with a private key) token is valid by trying to
    unsign it (with the corresponding public key).
    When valid, the claims is returned (typically containing user name).
    If it is not valid, an exception is thrown.")

  ;; TODO later
  ;; revoke-user
  ;; reset-user

  )






