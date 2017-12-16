(ns com.sixsq.slipstream.ssclj.resources.email.utils
  (:import (java.security MessageDigest)))

(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))
