(ns com.sixsq.slipstream.ssclj.middleware.authn-info-header
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

;; NOTE: ring uses lowercased values of header names!
(def ^:const authn-info-header
  "slipstream-authn-info")

(def ^:const authn-cookie
  "com.sixsq.slipstream.cookie")

(defn parse-authn-header
  [request]
  (seq (remove str/blank? (-> request
                              (get-in [:headers authn-info-header])
                              (or "")
                              (str/split #"\s+")))))

(defn extract-authn-info
  [request]
  (when-let [terms (parse-authn-header request)]
    (let [username (first terms)
          roles (set (rest terms))]
      [username roles])))

(defn is-session?
  "returns nil if the value does not look like a session; the session otherwise"
  [^String s]
  (if s
    (re-matches #"^session/.*" s)))

(defn extract-header-claims
  [request]
  (when-let [terms (parse-authn-header request)]
    (let [username (first terms)
          roles (seq (remove is-session? (rest terms)))
          session (first (keep is-session? (rest terms)))]
      (cond-> {}
              username (assoc :username username)
              roles (assoc :roles roles)
              session (assoc :session session)))))

(defn request-cookies [request]
  (get-in request [:cookies authn-cookie]))

(defn extract-cookie-claims [request]
  (cookies/extract-cookie-claims (request-cookies request)))

(defn extract-info [request]
  (or
    (extract-authn-info request)
    (cookies/extract-cookie-info (request-cookies request))))

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
  (let [[username roles] (extract-info request)]
    (-> request
        (assoc :user-name username)
        (assoc :user-roles roles))))

(defn add-claims
  [request]
  (if-let [claims (or
                    (extract-header-claims request)
                    (extract-cookie-claims request))]
    (assoc request :sixsq.slipstream.authn/claims claims)
    request))

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
         add-claims
         handler)))
