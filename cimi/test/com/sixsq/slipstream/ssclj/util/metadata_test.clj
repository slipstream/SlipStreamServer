(ns com.sixsq.slipstream.ssclj.util.metadata-test
  (:require
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key :as api-key-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud :as cloud-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-key-pair :as ssh-key-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key :as ssh-public-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata :as md]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-api-key :as api-key-session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github :as github]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal :as internal]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid :as mitreid]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid-token :as mitreid-token]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc :as oidc]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct :as direct]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-github-registration :as github-reg]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-mitreid-registration :as mitreid-reg]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-oidc-registration :as oidc-reg]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-self-registration :as self-reg]
    [com.sixsq.slipstream.ssclj.util.metadata :as t]
    ))


(deftest check-correct-metadata

  (are [spec] (stu/is-valid ::md/resource-metadata (t/generate "https://example.com/type" spec))

              ;; credential templates
              :cimi/credential-template.api-key
              :cimi/credential-template.ssh-key-pair
              :cimi/credential-template.ssh-public-key

              ;; session templates
              ::api-key-session/api-key
              ::github/github
              ::internal/internal
              ::mitreid/mitreid
              ::mitreid-token/mitreid-token
              ::oidc/oidc

              ;; user registration templates
              ::direct/direct
              ::github-reg/github-registration
              ::mitreid-reg/mitreid-registration
              ::oidc-reg/oidc-registration
              ::self-reg/self-registration
              ))

