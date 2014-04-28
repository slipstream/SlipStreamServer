(ns slipstream.credcache.utils
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io File]
    [java.security SecureRandom]
    [java.util UUID]
    [javax.xml.bind DatatypeConverter]))

(def rnd (SecureRandom.))

(defn random-char
  "Produces a random character from the Base 36 character set:
   digits and lowercase letters."
  []
  (-> (.nextInt rnd Character/MAX_RADIX)
      (Character/forDigit Character/MAX_RADIX)))

(defn random-string
  "Produces a string of random Base 36 characters of the length
   n.  If n is not positive, then the empty string will be
   returned."
  [n]
  (apply str (repeatedly n random-char)))

(defn file->byte-array
  "Reads the contents of a binary file and returns the contents
   as a byte array.  The argument can be either a File or a
   string giving the path of the file."
  [file]
  (io!
    (let [^File file (io/as-file file)]
      (with-open [reader (io/input-stream file)]
        (let [bytes (byte-array (.length file))]
          (.read reader bytes)
          bytes)))))

(defn byte-array->file
  "Writes the contents of the byte array to the given file.
   The argument can be either a File or a string giving the
   path of the file."
  [bytes file]
  (io!
    (let [^File file (io/as-file file)]
      (with-open [writer (io/output-stream file)]
        (.write writer bytes)))))

(defn bytes->base64
  "Converts the given byte array into a Base64-encoded string."
  [bytes]
  (DatatypeConverter/printBase64Binary bytes))

(defn base64->bytes
  "Converts the given Base64-encoded string into a byte array."
  [base64]
  (DatatypeConverter/parseBase64Binary base64))

(defn random-uuid
  "Returns the string form of a random UUID."
  []
  (str (UUID/randomUUID)))
