(ns com.sixsq.slipstream.ssclj.resources.service-attribute-test
    (:require
    [com.sixsq.slipstream.ssclj.resources.service-attribute :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(expect #"[\da-fA-F]+" (uri->id ""))                        ;; allowed here, not in Attribute schema
(expect #"[\da-fA-F]+" (uri->id "http://example.org/attributes"))
(expect #"[\da-fA-F]+" (uri->id "http://example.org/attributes_with_accents_ôéå"))
(expect #"[\da-fA-F]+" (uri->id "http://example.org/attributes#funky?query=/values"))

(expect Exception (crud/new-identifier {:prefix " "} resource-name))
(expect Exception (crud/new-identifier {:prefix "http://example.org/invalid uri"} resource-name))

(let [uri "example-org"
      name "price"
      hex (uri->id (str uri ":" name))
      id (str (u/de-camelcase resource-name) "/" hex)]
  (expect id (:id (crud/new-identifier {:prefix uri :attr-name name} resource-name))))

(def long-uri (apply str "http://" (repeat 10000 "a")))
(expect (str (u/de-camelcase resource-name) "/" (uri->id long-uri)))
