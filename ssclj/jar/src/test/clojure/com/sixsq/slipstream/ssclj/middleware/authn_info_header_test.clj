(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header-test
  (:require
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer :all]
    [expectations :refer :all]))

(expect [nil []] (extract-authn-info {}))
(expect [nil []] (extract-authn-info {:headers {"header-1" "value"}}))
(expect [nil []] (extract-authn-info {:headers {authn-info-header nil}}))
(expect [nil []] (extract-authn-info {:headers {authn-info-header ""}}))
(expect ["uname" []] (extract-authn-info {:headers {authn-info-header "uname"}}))
(expect ["uname" []] (extract-authn-info {:headers {authn-info-header "  uname"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "uname r1"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "  uname r1"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "uname r1  "}}))
(expect ["uname" ["r1" "r2"]] (extract-authn-info {:headers {authn-info-header "uname r1 r2"}}))

(expect {} (create-identity-map [nil []]))
(expect {} (create-identity-map [nil ["roles"]]))
(expect {:current         "uname"
         :authentications {"uname" {:identity "uname"}}} (create-identity-map ["uname" []]))
(expect {:current         "uname"
         :authentications {"uname" {:identity "uname"
                                    :roles    ["r1"]}}} (create-identity-map ["uname" ["r1"]]))
(expect {:current         "uname"
         :authentications {"uname" {:identity "uname"
                                    :roles    ["r1" "r2"]}}} (create-identity-map ["uname" ["r1" "r2"]]))

(let [handler (wrap-authn-info-header identity)]

  (expect {} (:identity (handler {})))
  (expect {} (:identity (handler {:headers {"header-1" "value"}})))
  (expect {} (:identity (handler {:headers {authn-info-header nil}})))
  (expect {} (:identity (handler {:headers {authn-info-header ""}})))
  (expect {:current         "uname"
           :authentications {"uname" {:identity "uname"}}}
          (:identity (handler {:headers {authn-info-header "uname"}})))
  (expect {:current         "uname"
           :authentications {"uname" {:identity "uname"
                                      :roles    ["r1"]}}}
          (:identity (handler {:headers {authn-info-header "uname r1"}})))
  (expect {:current         "uname"
           :authentications {"uname" {:identity "uname"
                                      :roles    ["r1" "r2"]}}}
          (:identity (handler {:headers {authn-info-header "uname r1 r2"}}))))

