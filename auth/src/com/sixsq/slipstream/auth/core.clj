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
    [this claims token]
    "Returns a token signing credentials or claims.

    * When called with credentials:
    Returns a token (signed with a private key) containing the given credentials, valid for a given time.
    If credentials are valid (see auth-user) [true, token] is returned (password being removed in token).
    Otherwise [false, message] is returned.

    * When called with claims and token:
    Returns a new token (signed with a private key) containing the given claims, valid for a given time.
     If given token is valid (see check-token) [true, claims-in-new-token] is returned.
     Otherwise [false, message] is returned.")

  (check-token
    [this token]
    "Verifies that the signed token (with a private key) is valid by trying to unsign it
    (with the corresponding public key).
    When valid, the claims are returned (e.g user name, or run id for a VM).
    If it is not valid, an exception is thrown.")

  ;; TODO later
  ;; revoke-user
  ;; reset-user

  )






