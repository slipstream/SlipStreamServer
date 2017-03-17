(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer :all]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [ring.util.codec :as codec))

(defn serialize-cookie-value
  "replaces the map cookie value with a serialized string"
  [{:keys [value] :as cookie}]
  (assoc cookie :value (codec/form-encode value)))

(def cookie-id (serialize-cookie-value (cookies/claims-cookie {:com.sixsq.identifier "uname2"})))
(def cookie-id-roles (serialize-cookie-value
                      (cookies/claims-cookie {:com.sixsq.identifier "uname2"
                                              :com.sixsq.roles      "USER alpha-role"})))

(deftest check-extract-authn-info
  (are [expected request] (= expected (extract-authn-info request))
                          nil {}
                          nil {:headers {"header-1" "value"}}
                          nil {:headers {authn-info-header nil}}
                          nil {:headers {authn-info-header ""}}
                          ["uname" []] {:headers {authn-info-header "uname"}}
                          ["uname" []] {:headers {authn-info-header "  uname"}}
                          ["uname" ["r1"]] {:headers {authn-info-header "uname r1"}}
                          ["uname" ["r1"]] {:headers {authn-info-header "  uname r1"}}
                          ["uname" ["r1"]] {:headers {authn-info-header "uname r1  "}}
                          ["uname" ["r1" "r2"]] {:headers {authn-info-header "uname r1 r2"}}))

(deftest check-extract-info
  (are [expected request] (= expected (extract-info request))
                          nil {}
                          ["uname" ["r1"]] {:headers {authn-info-header "uname r1"}}
                          ["uname2" ["USER" "alpha-role"]] {:cookies {authn-cookie cookie-id-roles}}
                          ["uname" ["r1"]] {:headers {authn-info-header "uname r1"}
                                            :cookies {authn-cookie cookie-id-roles}}))

(deftest check-identity-map
  (are [expected v] (= expected (create-identity-map v))
                    {} nil
                    {} [nil nil]
                    {} [nil []]
                    {} [nil ["roles"]]

                    {:current         "uname"
                     :authentications {"uname" {:identity "uname"}}}
                    ["uname" []]

                    {:current         "uname"
                     :authentications {"uname" {:identity "uname"
                                                :roles    ["r1"]}}}
                    ["uname" ["r1"]]

                    {:current         "uname"
                     :authentications {"uname" {:identity "uname"
                                                :roles    ["r1" "r2"]}}}
                    ["uname" ["r1" "r2"]]))

(deftest check-handler
  (let [handler (wrap-authn-info-header identity)]
    (are [expected request] (= expected (:identity (handler request)))
                            {} {}
                            {} {:headers {"header-1" "value"}}
                            {} {:headers {authn-info-header nil}}
                            {} {:headers {authn-info-header ""}}

                            {:current         "uname"
                             :authentications {"uname" {:identity "uname"}}}
                            {:headers {authn-info-header "uname"}}

                            {:current         "uname"
                             :authentications {"uname" {:identity "uname"
                                                        :roles    ["r1"]}}}
                            {:headers {authn-info-header "uname r1"}}

                            {:current         "uname"
                             :authentications {"uname" {:identity "uname"
                                                        :roles    ["r1" "r2"]}}}
                            {:headers {authn-info-header "uname r1 r2"}}

                            {:current         "uname2"
                             :authentications {"uname2" {:identity "uname2"}}}
                            {:cookies {authn-cookie cookie-id}}

                            {:current         "uname2"
                             :authentications {"uname2" {:identity "uname2"
                                                         :roles    ["USER" "alpha-role"]}}}
                            {:cookies {authn-cookie cookie-id-roles}}

                            {:current         "uname"
                             :authentications {"uname" {:identity "uname"
                                                        :roles    ["r1" "r2"]}}}
                            {:headers {authn-info-header "uname r1 r2"}
                             :cookies {authn-cookie cookie-id-roles}})))
