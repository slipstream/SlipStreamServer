(ns com.sixsq.slipstream.ssclj.resources.external-object-alpha-example
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.persistent-db :as pdb]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.auth.acl :as a])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const objectType "alpha")

;; Trivial example has the same schema as the template.  A real
;; resource may have different schemas for the template and resource.
(s/def :cimi/external-object.alpha :cimi/external-object-template.alpha)

(def ExternalObjectAlphaDescription tpl/ExternalObjectTemplateAlphaDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectAlphaDescription)

;;
;; multimethods for validation
;;


(def validate-fn (u/create-spec-validation-fn :cimi/external-object.alpha))


(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.alpha-create))


(defmethod eo/create-validate-subtype objectType
  [resource]
  (create-validate-fn resource))


(defn upload-fn
  [{state :state id :id :as resource} request]
  (if (= state eo/state-new)
    (do
      (log/warn "Requesting upload url for external object : " id)
      (assoc resource :state eo/state-ready :uri "file://foo"))
    (logu/log-and-throw-400 "Upload url request is not allowed")))


(defmethod eo/upload-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (let [id (str (u/de-camelcase eo/resource-name) "/" uuid)]
      (-> (pdb/retrieve id request)
          (upload-fn request)
          (pdb/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))


(defn download-fn
  [{state :state id :id :as resource} request]
  (if (= state eo/state-ready)
    (do
      (log/warn "Requesting download url for external object : " id)
      (assoc resource :uri "file://foo/bar"))
    (logu/log-and-throw-400 "Getting download  url request is not allowed")))


(defmethod eo/download-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (let [id (str (u/de-camelcase eo/resource-name) "/" uuid)]
      (-> (pdb/retrieve id request)
          (download-fn request)
          (pdb/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))

