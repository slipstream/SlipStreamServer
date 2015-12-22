(ns com.sixsq.slipstream.auth.app.http-utils)

(defn response
  [code]
  { :status code
    :headers {"Content-Type" "text/plain"}})

(defn response-with-body
  [code body]
  { :status code
    :headers {"Content-Type" "text/plain"}
    :body body})

(defn response-redirect
  [url]
  (-> 307
      response
      (assoc-in [:headers "location"] url)))

