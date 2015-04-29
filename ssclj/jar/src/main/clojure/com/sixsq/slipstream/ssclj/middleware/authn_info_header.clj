(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header
  (:require 
    [clojure.string                                             :as str]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]))

;; NOTE: ring uses lowercased values of header names!
(def ^:const authn-info-header
  "slipstream-authn-info")

(defn extract-authn-info
  [request]  
  (let [hcontent (get-in request [:headers authn-info-header])]
    (->> (str/split (or hcontent "") #"\s+")
         (remove str/blank?)
         ((juxt first rest)))))

(defn create-identity-map
  [[username roles]]
  (if username
    (let [id-map (if (seq roles) {:roles roles} {})
          id-map (assoc id-map :identity username)]
      {:current         username
       :authentications {username id-map}})
    {}))

(defn wrap-authn-info-header
  "Middleware that adds an identity map to the request based on
   information in the slipstream-authn-info header."
  [handler]
  (fn [request]
    (->> request
         (extract-authn-info)
         (create-identity-map)
         (assoc request :identity)                  
         (handler))))
