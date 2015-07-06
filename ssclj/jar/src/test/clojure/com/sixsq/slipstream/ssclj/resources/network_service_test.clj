(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test :refer :all]
    [peridot.core :refer :all]

    [com.sixsq.slipstream.ssclj.resources.test-utils      :refer [exec-request exec-post is-count]]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]))

(def base-uri (str p/service-context resource-name))

(deftest post-network-service
  (-> (exec-post base-uri "" "joe")
      (t/is-status 201)))