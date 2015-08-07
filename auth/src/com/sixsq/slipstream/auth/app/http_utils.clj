(ns com.sixsq.slipstream.auth.app.http-utils)

(defn response
  [code]
  { :status code
    :headers {"Content-Type" "text/plain"}})


