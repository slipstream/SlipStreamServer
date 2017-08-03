(ns com.sixsq.slipstream.ssclj.resources.credential.ssh-utils
  (:require
    [clojure.string :as str])
  (:import (com.jcraft.jsch JSch KeyPair)
           (java.io ByteArrayOutputStream)))

(def ^:const default-key-size 1024)

(def ^:const default-key-type :rsa)

(defn- get-public-key [^KeyPair kp]
  (with-open [baos (ByteArrayOutputStream.)]
    (let [fingerprint (.getFingerPrint kp)]
      (.writePublicKey kp baos fingerprint)
      (.toString baos "UTF-8"))))

(defn- get-private-key [^KeyPair kp]
  (with-open [baos (ByteArrayOutputStream.)]
    (.writePrivateKey kp baos)
    (.toString baos "UTF-8")))

(def key-name-to-index {"rsa" KeyPair/RSA
                        "dsa" KeyPair/DSA})

(def key-index-to-name {KeyPair/RSA "rsa"
                        KeyPair/DSA "dsa"})

(defn generate
  "Generates a new SSH key pair with the given type and length. The type can
   either be 'rsa' (default) or 'dsa'. The key size will default to 1024 bits.
   Note that not all cloud providers support DSA keys."
  ([]
   (generate :rsa default-key-size))
  ([type]
   (generate type default-key-size))
  ([type size]
   (let [type-key (-> type name str/lower-case)]
     (if-let [type-const (key-name-to-index type-key)]
       (try
         (let [kp (KeyPair/genKeyPair (JSch.) type-const size)
               fingerprint (.getFingerPrint kp)]
           {:type        type
            :fingerprint fingerprint
            :publicKey  (get-public-key kp)
            :privateKey (get-private-key kp)})
         (catch Exception e
           (throw (ex-info (str e) {}))))
       (throw (ex-info (str "invalid key type: " type-key) {}))))))

(defn load
  "Loads a public key (validating it in the process) and then returns a map
   with the public key information. The public-key parameter must be a string
   representation of the public key in a supported format."
  [public-key]
  (let [^bytes bytes (.getBytes public-key "UTF-8")]
    (try
      (let [kp (KeyPair/load (JSch.) nil bytes)
            key-type (.getKeyType kp)]
        (if-let [type-name (key-index-to-name key-type)]
          {:type        type-name
           :fingerprint (.getFingerPrint kp)
           :publicKey  (get-public-key kp)}
          (throw (ex-info (str "unsupported public key type: " key-type) {}))))
      (catch Exception e
        (throw (ex-info "invalid public key" {}))))))
