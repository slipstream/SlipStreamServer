(ns com.sixsq.slipstream.ssclj.resources.spec.session
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.session-template/method :cimi.core/identifier)
(s/def :cimi.session/username :cimi.core/nonblank-string)   ;; optional to support external authentication methods
(s/def :cimi.session/expiry :cimi.core/nonblank-string)     ;; uses cookie timestamp format

(s/def :cimi.session/server :cimi.core/nonblank-string)
(s/def :cimi.session/clientIP :cimi.core/nonblank-string)

(s/def :cimi.session/redirectURI :cimi.core/nonblank-string)

(s/def :cimi/session
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.session-template/method
                               :cimi.session/expiry]
                      :opt-un [:cimi.session/username
                               :cimi.session/server
                               :cimi.session/clientIP
                               :cimi.session/redirectURI]}))
