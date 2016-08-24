(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [superstring.core :as s]))

(defn read-str
  [s]
  (try
    (read-string s)
    (catch RuntimeException ex
      (if-not (s/starts-with? (.getMessage ex) "Invalid token")
        (throw ex)
        s))))

(defn display
  [d & [msg]]
  (println msg)
  (clojure.pprint/pprint d)
  d)
