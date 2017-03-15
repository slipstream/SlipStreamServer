(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.sign :as sign]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

;; NOTE: ring uses lowercased values of header names!
(def ^:const authn-info-header
  "slipstream-authn-info")

(def ^:const authn-cookie
  "com.sixsq.slipstream.cookie")

(defn extract-authn-info
  [request]
  (let [terms (remove str/blank? (-> request
                                     (get-in [:headers authn-info-header])
                                     (or "")
                                     (str/split #"\s+")))]
    (when (seq terms)
      ((juxt first rest) terms))))

(defn extract-info [request]
  (or
    (extract-authn-info request)
    (cookies/extract-cookie-info (get-in request [:cookies authn-cookie]))))

(defn create-identity-map
  [[username roles]]
  (if username
    (let [id-map (if (seq roles) {:roles roles} {})
          id-map (assoc id-map :identity username)]
      {:current         username
       :authentications {username id-map}})
    {}))

(defn add-user-name-roles
  [request]
  (let [[user-name roles] (extract-info request)]
    (-> request
        (assoc :user-name user-name)
        (assoc :user-roles roles))))

(defn wrap-authn-info-header
  "Middleware that adds an identity map to the request based on
   information in the slipstream-authn-info header or authentication
   cookie.  If both are provided, the header takes precedence."
  [handler]
  (fn [request]
    (->> request
         (extract-info)
         (create-identity-map)
         (assoc request :identity)
         add-user-name-roles
         (handler))))
