(ns com.sixsq.slipstream.ssclj.resources.user.utils
  (:require
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback-create-user-github :as user-github-callback]
    [com.sixsq.slipstream.ssclj.resources.callback-create-user-mitreid :as user-mitreid-callback]
    [com.sixsq.slipstream.ssclj.resources.callback-create-user-oidc :as user-oidc-callback]
    [com.sixsq.slipstream.ssclj.resources.callback-user-email-validation :as user-email-callback]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.util.response :as r]))


(def min-password-length 8)


(defn check-password-constraints
  [{:keys [password passwordRepeat]}]
  (cond
    (not (and password passwordRepeat)) (throw (r/ex-bad-request "both password fields must be specified"))
    (not= password passwordRepeat) (throw (r/ex-bad-request "password fields must be identical"))
    (< (count password) min-password-length) (throw (r/ex-bad-request (str "password must have at least " min-password-length " characters"))))
  true)


;; FIXME: Fix ugliness around needing to create ring requests with authentication!
(defn create-user-email-callback [user-id baseURI]
  (let [callback-request {:params   {:resource-name callback/resource-url}
                          :body     {:action         user-email-callback/action-name
                                     :targetResource {:href user-id}}
                          :identity {:current         "INTERNAL"
                                     :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                   :roles    ["ADMIN"]}}}}
        {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
    (if (= 201 status)
      (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
        (if-let [validate-op (u/get-op callback-resource "execute")]
          (str baseURI validate-op)
          (let [msg "callback does not have execute operation"]
            (throw (ex-info msg (r/map-response msg 500 resource-id)))))
        (let [msg "cannot retrieve email validation callback"]
          (throw (ex-info msg (r/map-response msg 500 resource-id)))))
      (let [msg "cannot create email validation callback"]
        (throw (ex-info msg (r/map-response msg 500 user-id)))))))


(defn create-user-oidc-callback
  ([baseURI href]
   (create-user-oidc-callback baseURI href nil))
  ([baseURI href data]
   (let [callback-request {:params   {:resource-name callback/resource-url}
                           :body     (cond-> {:action         user-oidc-callback/action-name
                                              :targetResource {:href href}}
                                             data (assoc :data data))
                           :identity {:current         "INTERNAL"
                                      :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                    :roles    ["ADMIN"]}}}}
         {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
     (if (= 201 status)
       (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
         (if-let [validate-op (u/get-op callback-resource "execute")]
           (str baseURI validate-op)
           (let [msg "callback does not have execute operation"]
             (throw (ex-info msg (r/map-response msg 500 resource-id)))))
         (let [msg "cannot retrieve user create callback"]
           (throw (ex-info msg (r/map-response msg 500 resource-id)))))
       (let [msg "cannot create user create callback"]
         (throw (ex-info msg (r/map-response msg 500 ""))))))))

(defn create-user-mitreid-callback
  ([baseURI href]
   (create-user-oidc-callback baseURI href nil))
  ([baseURI href data]
   (let [callback-request {:params   {:resource-name callback/resource-url}
                           :body     (cond-> {:action         user-mitreid-callback/action-name
                                              :targetResource {:href href}}
                                             data (assoc :data data))
                           :identity {:current         "INTERNAL"
                                      :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                    :roles    ["ADMIN"]}}}}
         {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
     (if (= 201 status)
       (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
         (if-let [validate-op (u/get-op callback-resource "execute")]
           (str baseURI validate-op)
           (let [msg "callback does not have execute operation"]
             (throw (ex-info msg (r/map-response msg 500 resource-id)))))
         (let [msg "cannot retrieve user create callback"]
           (throw (ex-info msg (r/map-response msg 500 resource-id)))))
       (let [msg "cannot create user create callback"]
         (throw (ex-info msg (r/map-response msg 500 ""))))))))

(defn create-user-github-callback
  ([baseURI href]
   (create-user-github-callback baseURI href nil))
  ([baseURI href data]
   (let [callback-request {:params   {:resource-name callback/resource-url}
                           :body     (cond-> {:action         user-github-callback/action-name
                                              :targetResource {:href href}}
                                             data (assoc :data data))
                           :identity {:current         "INTERNAL"
                                      :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                    :roles    ["ADMIN"]}}}}
         {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
     (if (= 201 status)
       (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
         (if-let [validate-op (u/get-op callback-resource "execute")]
           (str baseURI validate-op)
           (let [msg "callback does not have execute operation"]
             (throw (ex-info msg (r/map-response msg 500 resource-id)))))
         (let [msg "cannot retrieve user create callback"]
           (throw (ex-info msg (r/map-response msg 500 resource-id)))))
       (let [msg "cannot create user callback"]
         (throw (ex-info msg (r/map-response msg 500 ""))))))))

(defn create-user-identifier
  []
  (crud/add {})
  )
