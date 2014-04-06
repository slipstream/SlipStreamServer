(ns slipstream.egipki.myproxy
  (:require
    [slipstream.egipki.utils :refer [random-string file->byte-array]])
  (:import [java.io File]
           [java.security SecureRandom]
           [org.ietf.jgss GSSCredential]
           [org.gridforum.jgss ExtendedGSSManager ExtendedGSSCredential]
           [org.globus.myproxy MyProxy]))

(def ttl-5m 500) ;; 5 minutes
(def ttl-4h 14400) ;; 4 hours

(defn credential-from-file
  "Loads the credential from a file."
  ^GSSCredential [file]
  (let [bytes (file->byte-array file)]
    (.. (ExtendedGSSManager/getInstance)
        (createCredential bytes
                          ExtendedGSSCredential/IMPEXP_OPAQUE
                          GSSCredential/DEFAULT_LIFETIME
                          nil ;; uses GSI as default
                          GSSCredential/INITIATE_AND_ACCEPT))))

(defn get-credential
  "Gets the credental from the MyProxy server.  All of the parameters
   are passed as a map with the following keys: :host, :port,
   :username, and :passphrase.  Defaults can be used for all
   parameters except :username and :passphrase."
  [{:keys [host port username passphrase]
    :or   {host "myproxy.grif.fr"
           port MyProxy/DEFAULT_PORT}}]
  (.. (MyProxy. host port)
      (get username passphrase ttl-4h)))

(defn put-credential
  "Puts a credential on the MyProxy server.  All of the parameters
   are passed as a map with the following keys: :credential, :host,
   :port, :username, :passphrase, and :lifetime (in seconds).
   Defaults can be used for all of the parameters except the
   credential.  The function returns a map of the values actually
   used."
  [{:keys [host port credential username passphrase lifetime]
    :or   {host       "myproxy.grif.fr"
           port       MyProxy/DEFAULT_PORT
           username   (random-string 8)
           passphrase (random-string 16)
           lifetime   ttl-5m}}]
  (.. (MyProxy. host port)
      (put credential username passphrase lifetime))
  {:host       host
   :port       port
   :username   username
   :passphrase passphrase
   :lifetime   lifetime})
