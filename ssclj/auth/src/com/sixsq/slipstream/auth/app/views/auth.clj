(ns com.sixsq.slipstream.auth.app.views.auth
  (:require
    [hiccup.core                          :refer :all]
    [hiccup.page                          :refer :all]
    [hiccup.form                          :refer :all]
    [com.sixsq.slipstream.auth.app.routes :as r]))

(defn- view-layout [& content]
  (html
    (doctype :xhtml-strict)
    (xhtml-tag "en"
               [:head
                [:meta {:http-equiv "Content-type"
                        :content "text/html; charset=utf-8"}]
                [:title "Login"]]
                [:body content])))

(defn register-form
  []
  (view-layout
    [:h2 "Register a new user"]
    [:form {:method "post" :action r/uri-register}
     [:div (label "user-name" "Name:")]
     [:input.add-user {:type "text" :name "user-name"}] [:br]

     [:div (label "password" "Password:")]
     [:input.add-user {:type "password" :name "password"}]  [:br]
     [:input.action {:type "submit" :value "register"}]]))

(defn registered-ok
  [user-name]
  (view-layout
    [:h2 user-name]
    " registered OK."))

(defn login
  []
  (view-layout
    [:h2 "Login"]
    [:form {:method "post" :action r/uri-login}
     [:div (label "user-name" "Name:")]
     [:input.login {:type "text" :name "user-name"}] [:br]

     [:div (label "password" "Password:")]
     [:input.login {:type "password" :name "password"}]  [:br]
     [:input.action {:type "submit" :value "login"}]]))

(defn login-ok
  [result]
  (view-layout
    [:h2 result]
    " login OK."))

(defn login-error
  [message]
  (view-layout
    [:h2 "Error in login"]
    (:message message)))
