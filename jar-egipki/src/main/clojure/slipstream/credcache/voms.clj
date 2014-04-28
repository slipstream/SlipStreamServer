(ns slipstream.credcache.voms
  "Utility functions to deal with VOMS servers and proxies."
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.io :as io]
    [slipstream.credcache.utils :as u])
  (:import
    [org.italiangrid.voms.credential UserCredentials]
    (org.italiangrid.voms.util CertificateValidatorBuilder CredentialsUtils CredentialsUtils$PrivateKeyEncoding)
    (org.italiangrid.voms.request.impl DefaultVOMSACService$Builder DefaultVOMSACRequest$Builder)
    (org.italiangrid.voms.request VOMSRequestListener)
    (org.ietf.jgss GSSCredential)
    (eu.emi.security.authn.x509.proxy ProxyCertificateOptions ProxyGenerator)
    (eu.emi.security.authn.x509.proxy ProxyCertificateOptions ProxyGenerator)
    (java.util ArrayList Date)
    (java.io ByteArrayInputStream ByteArrayOutputStream)
    (eu.emi.security.authn.x509.impl KeyAndCertCredential PEMCredential)
    (org.globus.gsi GlobusCredential)
    (org.globus.gsi.gssapi GlobusGSSCredentialImpl)
    (org.bouncycastle.asn1.x509 AttributeCertificate)
    (java.security.cert X509Certificate)
    (eu.emi.security.authn.x509 X509Credential)
    [eu.emi.security.authn.x509.proxy ProxyType ProxyGenerator ProxyCertificateOptions]))

;;
;; example workflow
;;
;; (def pswd "changeme")
;; (def cred (voms/get-user-cred pswd))
;; (def ac (voms/get-attr-certs cred))
;; (def pxy (voms/add-attr-certs cred ac))
;; (voms/write-proxy "proxy.pem" pxy)

(defn get-request-listener []
  (reify VOMSRequestListener
    (notifyVOMSRequestStart [this request server-info]
      (log/info request server-info))
    (notifyVOMSRequestSuccess [this request endpoint]
      (log/info request endpoint))
    (notifyVOMSRequestFailure [this request endpoint error]
      (log/error request endpoint error))
    (notifyErrorsInVOMSReponse [this request server-info errors]
      (log/error request server-info errors))
    (notifyWarningsInVOMSResponse [this request server-info warnings]
      (log/warn request server-info warnings))))

(defn get-user-x509cred
  [passphrase]
  (UserCredentials/loadCredentials (char-array passphrase)))

(defn globuscred->x509cred
  [^GlobusCredential gsscred]
  (let [key (.getPrivateKey gsscred)
        chain (.getCertificateChain gsscred)]
    (KeyAndCertCredential. key chain)))

(defn gsscred->x509cred
  [^GlobusGSSCredentialImpl gsscred]
  (let [key (.getPrivateKey gsscred)
        chain (.getCertificateChain gsscred)]
    (KeyAndCertCredential. key chain)))

(defn x509cred->gsscred
  [^X509Credential x509cred]
  (let [key (.getKey x509cred)
        chain (.getCertificateChain x509cred)]
    (-> (org.globus.gsi.X509Credential. key chain)
        (GlobusGSSCredentialImpl. GSSCredential/INITIATE_AND_ACCEPT))))

(defn generate-proxy
  [lifetime passphrase]
  (let [lifetime (or lifetime ProxyCertificateOptions/DEFAULT_LIFETIME)
        credential (->> passphrase
                        (get-user-x509cred))
        private-key (.getKey credential)
        chain (.getCertificateChain credential)
        options (doto (ProxyCertificateOptions. chain)
                  #_(.setKeyLength ProxyCertificateOptions/DEFAULT_LONG_KEY_LENGTH)
                  (.setKeyLength 512)
                  (.setType ProxyType/RFC3820)
                  (.setLifetime lifetime)
                  (.setProxyPathLimit 0xffff)
                  #_(.setProxyKeyUsageMask (int 2r11101)))]
    (ProxyGenerator/generate options private-key)))

(defn voms-service
  []
  (-> (CertificateValidatorBuilder/buildCertificateValidator)
      (DefaultVOMSACService$Builder.)
      (.requestListener (get-request-listener))
      (.build)))

(defn voms-request
  [vo fqans targets]
  (-> (DefaultVOMSACRequest$Builder. vo)
      (.fqans fqans)
      (.targets targets)
      (.build)))

(defn get-ac
  "Gets an attribute certificate from a VOMS server for the given VO,
   FQANs (roles), and targets."
  [cred vo {:keys [fqans targets]
            :or   {:fqans   []
                   :targets []}}]
  (let [service (voms-service)
        request (voms-request vo fqans targets)]
    (.getVOMSAttributeCertificate service cred request)))

(defn get-acs
  "Returns an array of attribute certificates from the given virtual organizations.
   An empty array is returned if there are no VOs given.  The vos parameter must
   be a map with the keys corresponding to VO names and the value is a map with
   :fqans or :targets keys containing lists of strings."
  [cred & [vos]]
  (->> (or vos {})
       (map (fn [[vo tags]] (get-ac cred vo tags)))
       (filter (complement nil?))
       (into-array AttributeCertificate)))

(defn add-acs
  "Adds the attribute certificates to the given credential, returning the
   combined proxy."
  [cred acs]
  (let [proxy-cert-options (-> (.getCertificateChain cred)
                               (ProxyCertificateOptions.))]
    (.setAttributeCertificates proxy-cert-options acs)
    (ProxyGenerator/generate proxy-cert-options (.getKey cred))))

(defn include-attr-certs
  [x509cred & [vos]]
  (->> (get-acs x509cred vos)
       (add-acs x509cred)))

(defn expiry-date
  "Get the expiry date of the proxy as the number of seconds since
   the epoch."
  [proxy]
  (let [millis (.. proxy
                   (getCredential)
                   (getCertificate)
                   (getNotAfter)
                   (getTime))]
    (quot millis 1000)))

(defn time-remaining
  "Remaining validity time (in seconds) for the proxy."
  [proxy]
  (- (expiry-date proxy) (quot (System/currentTimeMillis) 1000)))

(defn write-proxy
  "Writes the proxy to the given output stream os."
  [os proxy]
  (->> proxy
       (.getCredential)
       (CredentialsUtils/saveProxyCredentials os)))

(defn read-proxy
  "Reads a password-less proxy from the given input stream."
  [is]
  (PEMCredential. is (char-array nil)))

(defn proxy->file
  "Writes the proxy into the given file."
  [proxy file]
  (with-open [os (io/output-stream file)]
    (write-proxy os proxy)))

(defn file->proxy
  "Recovers the proxy from the given file."
  [file]
  (with-open [is (io/input-stream file)]
    (read-proxy is)))

(defn proxy->bytes
  "Returns the proxy credentials as a byte array."
  [proxy]
  (with-open [os (ByteArrayOutputStream.)]
    (write-proxy os proxy)
    (.toByteArray os)))

(defn bytes->proxy
  "Recovers the proxy from a byte array."
  [bytes]
  (with-open [is (ByteArrayInputStream. bytes)]
    (read-proxy is)))

(defn proxy->cred-info
  "Creates the map containing the credential information for a VOMS proxy.
   The optional map contains all of the parameters required to review the
   proxy.  Note that the :credential and :expiry values will be overwritten
   if present."
  [proxy & [renewal-params]]
  (let [cred-info (or renewal-params {})
        expiry (expiry-date proxy)
        base64 (-> proxy
                   (proxy->bytes)
                   (u/bytes->base64))]
    (assoc cred-info :credential base64 :expiry expiry)))

(defn cred-info->proxy
  "Recovers the VOMS proxy from the given credential information."
  [cred-info]
  (-> (:credential cred-info)
      (u/base64->bytes)
      (bytes->proxy)))

