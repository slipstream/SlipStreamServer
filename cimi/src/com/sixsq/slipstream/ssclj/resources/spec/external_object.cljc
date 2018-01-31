(ns com.sixsq.slipstream.ssclj.resources.spec.external-object

    (:require
      [clojure.spec.alpha :as s]
      [com.sixsq.slipstream.ssclj.resources.spec.external-object-template])
    )

;;
;; Note that all of the keys and keys specs are already defined
;; with the ExternalObjectTemplate.  This file exists only to allow a
;; place to define new keys, if that should become necessary.
;;
;; As for the ExternalObjectTemplate, this is a "base class" so there
;; is no sense in defining map resources for the resource itself.
;;
