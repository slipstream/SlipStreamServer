(ns com.sixsq.slipstream.ssclj.resources.spec.session
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))


;; reference to the session template that was used to create the session
(s/def :cimi.session/sessionTemplate (s/merge
                                       (s/keys :req-un [:cimi.session-template/href])
                                       (s/map-of #{:href} any?)))

;; expiration time of the cookie
;; uses cookie timestamp format
(s/def :cimi.session/expiry :cimi.core/nonblank-string)

;; username is optional to support external authentication methods
;; that usually require creation of stub session for later validation
(s/def :cimi.session/username :cimi.core/nonblank-string)

(s/def :cimi.session/server :cimi.core/nonblank-string)
(s/def :cimi.session/clientIP :cimi.core/nonblank-string)

;; supports use of the resource through browser clients,
;; specifically allows use of form submit buttons
(s/def :cimi.session/redirectURI :cimi.core/nonblank-string)

(s/def :cimi/session
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.session-template/method
                               :cimi.session/sessionTemplate
                               :cimi.session/expiry]
                      :opt-un [:cimi.session/username
                               :cimi.session/server
                               :cimi.session/clientIP
                               :cimi.session/redirectURI]}))
