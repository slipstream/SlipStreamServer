(ns slipstream.egipki.utils
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io File]
    [java.security SecureRandom]))

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

