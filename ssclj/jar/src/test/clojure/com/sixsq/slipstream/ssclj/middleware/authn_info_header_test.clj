(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header-test
  (:require
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer :all]
    [com.sixsq.slipstream.auth.sign :as sign]
    [expectations :refer :all]))

(defn mk-cookie [token]
  {authn-cookie {:value token
                 :path  "/"}})

(def cookie-id (mk-cookie (sign/sign-claims {:com.sixsq.identifier "uname2"})))
(def cookie-id-roles (mk-cookie (sign/sign-claims {:com.sixsq.identifier "uname2"
                                                   :com.sixsq.roles      "USER alpha-role"})))

(expect nil (extract-authn-info {}))
(expect nil (extract-authn-info {:headers {"header-1" "value"}}))
(expect nil (extract-authn-info {:headers {authn-info-header nil}}))
(expect nil (extract-authn-info {:headers {authn-info-header ""}}))
(expect ["uname" []] (extract-authn-info {:headers {authn-info-header "uname"}}))
(expect ["uname" []] (extract-authn-info {:headers {authn-info-header "  uname"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "uname r1"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "  uname r1"}}))
(expect ["uname" ["r1"]] (extract-authn-info {:headers {authn-info-header "uname r1  "}}))
(expect ["uname" ["r1" "r2"]] (extract-authn-info {:headers {authn-info-header "uname r1 r2"}}))

(expect nil (extract-cookie-info {}))
(expect nil (extract-cookie-info {:cookies {}}))
(expect nil (extract-cookie-info {:cookies {authn-cookie {}}}))
(expect nil (extract-cookie-info {:cookies {authn-cookie {:value nil}}}))
(expect ["uname2" []] (extract-cookie-info {:cookies cookie-id}))
(expect ["uname2" ["USER" "alpha-role"]] (extract-cookie-info {:cookies cookie-id-roles}))

(expect nil (extract-info {}))
(expect ["uname" ["r1"]] (extract-info {:headers {authn-info-header "uname r1"}}))
(expect ["uname2" ["USER" "alpha-role"]] (extract-info {:cookies cookie-id-roles}))
(expect ["uname" ["r1"]] (extract-info {:headers {authn-info-header "uname r1"}
                                        :cookies cookie-id-roles}))

(expect {} (create-identity-map nil))
(expect {} (create-identity-map [nil nil]))
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
          (:identity (handler {:headers {authn-info-header "uname r1 r2"}})))
  (expect {:current         "uname2"
           :authentications {"uname2" {:identity "uname2"}}}
          (:identity (handler {:cookies cookie-id})))
  (expect {:current         "uname2"
           :authentications {"uname2" {:identity "uname2"
                                       :roles    ["USER" "alpha-role"]}}}
          (:identity (handler {:cookies cookie-id-roles})))
  (expect {:current         "uname"
           :authentications {"uname" {:identity "uname"
                                      :roles    ["r1" "r2"]}}}
          (:identity (handler {:headers {authn-info-header "uname r1 r2"}
                               :cookies cookie-id-roles}))))

