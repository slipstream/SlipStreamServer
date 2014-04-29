(ns slipstream.credcache.common-test
  (:require
    [expectations :refer :all]
    [slipstream.credcache.common :refer :all]))

(def ^:const credential-type-uri "http://schemas.dmtf.org/cimi/1/Credential")
(def ^:const credential-template-type-url "http://schemas.dmtf.org/cimi/1/CredentialTemplate")
(def ^:const credential-proxy-type-uri "http://schemas.dmtf.org/cimi/1/Credential#proxy")
(def ^:const credential-template-proxy-type-url "http://schemas.dmtf.org/cimi/1/CredentialTemplate#proxy")

;; update-timestamps
(let [initial (update-timestamps {:other "OK"})
      _ (Thread/sleep 3000)
      updated (update-timestamps initial)]
  (expect (complement nil?) (:updated initial))
  (expect (complement nil?) (:created initial))
  (expect (complement nil?) (:updated updated))
  (expect (complement nil?) (:created updated))
  (expect (:created initial) (:created updated))
  (expect (not= (:updated initial) (:updated updated)))
  (expect "OK" (:other initial))
  (expect "OK" (:other updated)))

;; get resource-type-uri
(expect credential-type-uri (get-resource-typeuri credential-template-type-url))
(expect credential-proxy-type-uri (get-resource-typeuri credential-template-proxy-type-url))

;; default template->resource implementation
(expect {:field "OK"} (template->resource {:field "OK"}))
(expect {:field "OK" :typeURI "unknown"} (template->resource {:field "OK" :typeURI "unknown"}))
(expect {:field "OK" :typeURI credential-type-uri}
        (template->resource {:field "OK" :typeURI credential-template-type-url}))
(expect {:field "OK" :typeURI credential-proxy-type-uri}
        (template->resource {:field "OK" :typeURI credential-template-proxy-type-url}))

;; default validate implementation
(expect RuntimeException
        (from-each [m [{}
                       {:typeURI "unknown"}
                       {:typeURI "unknown" :other "field"}]]
                   (validate m)))

(expect-let [m {}]
            m
            (try
              (validate m)
              (catch Exception e
                (ex-data e))))

(expect-let [m {:typeURI "unknown" :other "field"}]
            m
            (try
              (validate m)
              (catch Exception e
                (ex-data e))))
