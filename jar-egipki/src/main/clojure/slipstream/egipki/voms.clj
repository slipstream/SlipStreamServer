(ns slipstream.egipki.voms
  (:require
    [clojure.java.io :as io])
  (:import
    [org.italiangrid.voms.credential UserCredentials]
    (org.italiangrid.voms.util CertificateValidatorBuilder CredentialsUtils)
    (org.italiangrid.voms.request.impl DefaultVOMSACService$Builder DefaultVOMSACRequest$Builder)
    (org.italiangrid.voms.request VOMSRequestListener)
    (eu.emi.security.authn.x509.proxy ProxyCertificateOptions ProxyGenerator)
    (org.bouncycastle.asn1.x509 AttributeCertificate)
    (java.util ArrayList)))

(defn get-request-listener []
  (reify VOMSRequestListener
    (notifyVOMSRequestStart [this request server-info]
      (println request server-info))
    (notifyVOMSRequestSuccess [this request endpoint]
      (println request endpoint))
    (notifyVOMSRequestFailure [this request endpoint error]
      (println request endpoint error))
    (notifyErrorsInVOMSReponse [this request server-info errors]
      (println request server-info errors))
    (notifyWarningsInVOMSResponse [this request server-info warnings]
      (println request server-info warnings))))

(defn get-user-cred
  [passphrase]
  (UserCredentials/loadCredentials (char-array passphrase)))

(defn get-attr-certs
  [cred]
  (let [cert-validator (CertificateValidatorBuilder/buildCertificateValidator)
        service (.. (DefaultVOMSACService$Builder. cert-validator)
                    (requestListener (get-request-listener))
                    (build))
        request (.. (DefaultVOMSACRequest$Builder. "vo.lal.in2p3.fr")
                    (build))]
    (.getVOMSAttributeCertificate service cred request)))

(defn add-attr-certs
  [cred attr-certs]
  (let [proxy-cert-options (-> (.getCertificateChain cred)
                               (ProxyCertificateOptions.))]
    (.setAttributeCertificates proxy-cert-options (into-array AttributeCertificate [attr-certs]))
    (ProxyGenerator/generate proxy-cert-options (.getKey cred))))

(defn write-proxy [file proxy]
  (let [os (io/output-stream file)
        credentials (.getCredential proxy)]
    (CredentialsUtils/saveProxyCredentials os credentials)))
