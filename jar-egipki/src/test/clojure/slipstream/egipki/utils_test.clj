(ns slipstream.egipki.utils-test
  (:require
    [expectations :refer :all]
    [slipstream.egipki.utils :refer :all])
  (:import
    [java.io File]))

;; random-char
(expect Character (random-char))

;; random-string
(expect "" (random-string 0))
(expect "" (random-string -1))
(expect String (random-string 1))
(expect #"^[\da-z]{27}$" (random-string 27))

;; file->byte-array, byte-array->file
(let [tfile (File/createTempFile "egipki" ".bin")
      bytes (byte-array (map byte (repeatedly 1024 random-char)))]
  (expect nil (byte-array->file bytes tfile))
  (expect (seq bytes) (seq (file->byte-array tfile))))

(let [tpath (.getPath (File/createTempFile "egipki" ".bin"))
      bytes (byte-array (map byte (repeatedly 1024 random-char)))]
  (expect nil (byte-array->file bytes tpath))
  (expect (seq bytes) (seq (file->byte-array tpath))))


