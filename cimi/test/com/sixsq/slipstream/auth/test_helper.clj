(ns com.sixsq.slipstream.auth.test-helper
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.user :as ur]
    [com.sixsq.slipstream.ssclj.resources.user-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.user-template-auto :as auto]
    [com.sixsq.slipstream.auth.internal :as ia]))

(def rname ur/resource-url)
(def req-u-name "unknown")
(def req-u-role "ANON")
(def req-template {:userTemplate
                   {:href (str ct/resource-url "/" auto/registration-method)
                    :method "auto"}})
(def request-base {:identity     {:current req-u-name
                                  :authentications
                                           {req-u-name {:roles    #{req-u-role}
                                                        :identity req-u-name}}}
                   :sixsq.slipstream.authn/claims
                                 {:username req-u-name
                                  :roles    #{req-u-role}}
                   :user-name    req-u-name
                   :params       {:resource-name rname}
                   :route-params {:resource-name rname}
                   :body         req-template})


(defn- user-request
  [user]
  (let [with-hashed-pass (assoc user :password (ia/hash-password (:password user)))
        request          (update-in request-base [:body :userTemplate] merge
                                    with-hashed-pass)]
    request))


(defn add-user-for-test!
  [user]
  (crud/add (user-request user)))


(defn es-db-dump
  [type]
  (println "ES DUMP. Doc type:" type)
  (clojure.pprint/pprint
    (esu/dump esb/*client* esb/index-name type))
  (println (apply str (repeat 20 "-"))))

