(ns com.sixsq.slipstream.ssclj.middleware.logger-test
  (:require
    [clojure.test :refer :all]
    [superstring.core :as str]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer :all]))

(def start 1450701200947)
(def end 1450701203089)

(defn header-authn-info
  [user roles]
  {"slipstream-authn-info" (str/join " " (concat [user] roles))})

(defn req
  [{:keys [user roles query-string]}]
  {:request-method :get
   :uri            "auth/login"
   :headers        (header-authn-info user roles)
   :query-string   query-string
   :body           "body-content"})

(defn is-request-formatted
  [expected & {:as req-params}]
  (is (= expected (formatted-request (req req-params)))))

(defn is-reponse-formatted
  [expected & {:keys [:start :end :status] :as response-params}]
  (is (= expected (formatted-response
                    (formatted-request (req response-params))
                     {:status status} start end))))

(deftest log-does-not-display-password
  (is-request-formatted "GET auth/login [super/ADMIN] ?a=1&b=2 body-content"
                        :user "super" :roles ["ADMIN"] :query-string "a=1&password=secret&b=2"))

(deftest test-formatting
  (is-reponse-formatted (format "200 (%d ms) GET auth/login [super/ADMIN] ?a=1&b=2 body-content" (- end start))
                        :user "super" :roles ["ADMIN"] :query-string "a=1&password=secret&b=2"
                        :start start :end end :status 200)

  (is-request-formatted "GET auth/login [joe/] ?c=3 body-content"
                        :user "joe" :query-string "c=3")

  (is-request-formatted "GET auth/login [joe/] ? body-content"
                        :user "joe")

  (is-request-formatted "GET auth/login [joe/R1,R2] ? body-content"
                        :user "joe" :roles ["R1" "R2"]))


