(ns com.sixsq.slipstream.ssclj.resources.credential-ssh-public-key
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key :as tpl]
    [com.sixsq.slipstream.ssclj.resources.credential.ssh-utils :as ssh-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-ssh-public-key]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.auth.acl :as a]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c])
  (:import (clojure.lang ExceptionInfo)))

(defn import-key [common-info publicKey]
  [nil (merge (ssh-utils/load publicKey) common-info)])

(defn generate-key [common-info algorithm size]
  (let [ssh-key (merge (ssh-utils/generate algorithm size) common-info)]
    [(select-keys ssh-key #{:privateKey}) (dissoc ssh-key :privateKey)]))

;;
;; convert template to credential: loads and validates the given SSH public key
;; provides attributes about the key.
;;
(defmethod p/tpl->credential tpl/credential-type
  [{:keys [type method publicKey algorithm size]} request]
  (let [common-info {:resourceURI p/resource-uri
                     :type        type
                     :method      method
                     :enabled     true}]
    (try
      (if publicKey
        (import-key common-info publicKey)
        (generate-key common-info algorithm size))
      (catch Exception e
        (logu/log-and-throw-400 (str "error creating SSH public key credential: '" (.getMessage e) "'"))))))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential.ssh-public-key))
(defmethod p/validate-subtype tpl/credential-type
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/credential.ssh-public-key.create))
(defmethod p/create-validate-subtype tpl/credential-type
  [resource]
  (create-validate-fn resource))


;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize p/resource-url :cimi/credential.ssh-public-key))

;;
;; Disable operation
;;
(defn disable-fn [{enabled :enabled id :id :as credential}]
  (if (or enabled (nil? enabled))
    (do
      (log/warn "Disabling credential : " id)
      (assoc credential :enabled false))
    (logu/log-and-throw-400 (str "Bad enabled field value " enabled))))


(defmethod p/disable-subtype tpl/credential-type
  [_ {{uuid :uuid} :params :as request}]
  (try
    (let [id (str (u/de-camelcase p/resource-name) "/" uuid)]
      (-> (db/retrieve id request)
          (a/can-modify? request)
          (disable-fn)
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))


;; Set operation
(def set-subtype-ops-fn
  (fn [{:keys [id] :as resource} request]
    (let [
          href-disable (str id "/disable")
          disable-op {:rel (:disable c/action-uri) :href href-disable}]
      (-> (crud/set-standard-operations resource request)
          (update-in [:operations] conj disable-op)))))

(defmethod p/set-subtype-ops tpl/credential-type
  [resource request]
  (set-subtype-ops-fn resource request))

