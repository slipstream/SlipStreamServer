(ns com.sixsq.slipstream.auth.utils.http)

(defn response
  [code]
  {:status  code
   :headers {"Content-Type" "text/plain"}})

(defn response-with-body
  [code body]
  {:status  code
   :headers {"Content-Type" "text/plain"}
   :body    body})

(defn response-redirect
  [url]
  (-> 307
      response
      (assoc-in [:headers "location"] url)))

(defn response-created
  "Provides a created response (201) with the Location header given by the
   identifier and provides the Set-Cookie header with the given cookie, if
   the cookie value is not nil."
  [id & [[cookie-name cookie]]]
  (cond-> {:status 201, :headers {"Location" id}}
          cookie (assoc :cookies {cookie-name cookie})))

;; FIXME: This should be 403 (invalid credentials), not 401 (credentials required)
(defn response-forbidden
  []
  (response 401))

(defn param-value
  [request key]
  (-> request
      :params
      (get key)))

(defn select-in-params
  [request keys]
  (-> request
      :params
      (select-keys keys)))

