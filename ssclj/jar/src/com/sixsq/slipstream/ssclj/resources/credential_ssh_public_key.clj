(ns com.sixsq.slipstream.ssclj.resources.credential-ssh-public-key
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.credential-ssh-public-key]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key :as tpl]
    [com.sixsq.slipstream.ssclj.resources.credential.ssh-utils :as ssh-utils]))

(defn import-key [common-info publicKey]
  [nil (-> (ssh-utils/load publicKey)
           (merge common-info))])

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
                     :method      method}]
    (if publicKey
      (import-key common-info publicKey)
      (generate-key common-info algorithm size))))

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
