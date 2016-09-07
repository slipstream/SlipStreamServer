(ns com.sixsq.slipstream.db.serializers.service-config-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [me.raynes.fs :as fs]
    [schema.core :as sch]
    [com.sixsq.slipstream.ssclj.util.config :as ssclj-cu]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as crs]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as crtpls]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as sd]
    [com.sixsq.slipstream.db.serializers.test-utils :as tu]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]
    [com.sixsq.slipstream.db.serializers.service-config :as scs]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [clojure.java.io :as io]
    [com.sixsq.slipstream.db.serializers.utils :as u])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    ))

(def sc-from-xml (-> "configuration.xml"
                     io/resource
                     .getPath
                     slurp
                     scu/conf-xml->sc))

(def sc-from-xml-params (.getParameters sc-from-xml))

(defn no-extra-param-r->p
  [m]
  (apply dissoc m (keys sci/extra-rname->param)))

(defn no-extra-param-p->r
  [m]
  (apply dissoc m (vals sci/extra-rname->param)))

(defn- keys-not-in-conf
  [conf]
  (for [k (-> sci/rname->param no-extra-param-r->p keys) :when (not (contains? conf k))]
    k))

(defn msg-keys-not-in-cfg
  [keys]
  (str "The following keys are not in config " (clojure.string/join ", " keys)))

(defn sc-param-desc-as-map
  [sc k]
  (let [p (.getParameter sc k)]
    (u/desc-from-param p)))

(defn param-value
  [sc k]
  (if-let [p (.getParameter sc k)]
    (.getValue p)
    ""))


;; Fixtures
(use-fixtures :once tu/fixture-start-es-db)


;; Tests

;; Test transformation from ServiceConfiguration to
;; configuration/slipstream resource document.
(deftest test-sc->cfg
  (is (= {} (sci/sc->cfg (ServiceConfiguration.))))
  (let [conf (sci/sc->cfg sc-from-xml)
        not-in-conf (keys-not-in-conf conf)]
    (is (empty? not-in-conf) (msg-keys-not-in-cfg not-in-conf))))

;; Test schema compliance after transformation of SC to
;; configuration/slipstream resource document.
(deftest test-check-sc-schema
  (is (nil? (sch/check crs/Configuration (sci/complete-resource (sci/sc->cfg sc-from-xml))))))

(deftest test-attrs-map-valid
  (is (= (set (keys (no-extra-param-r->p sci/rname->param)))
         (set (keys crtpls/config-attrs)))))

(deftest test-fail-store-if-no-default-in-db
  (is (thrown? RuntimeException (scs/store (ServiceConfiguration.)))))

(deftest test-save-load-values-ok
  (let [_ (sci/db-add-default-config)
        sc-to-es (scs/store sc-from-xml)
        sc-from-es (scs/load)]
    (is (not (nil? sc-from-es)))
    (doseq [k (keys (no-extra-param-p->r sci/param->rname))]
      (is (= (param-value sc-to-es k)
             (param-value sc-from-es k))))))

(def conf-desc-file (str "test-resources/" (fs/temp-name "conf-desc-" ".edn")))

(deftest test-save-load-desc-ok
  (let [_ (sci/db-add-default-config)
        _ (sci/cs->cfg-desc-and-spit sc-from-xml conf-desc-file)
        _ (scs/store sc-from-xml)
        sc-from-es (with-redefs-fn {#'com.sixsq.slipstream.db.serializers.service-config-impl/load-cfg-desc
                                    (fn [] (ssclj-cu/read-config conf-desc-file))}
                     #(scs/load))
        _ (fs/delete conf-desc-file)]
    (doseq [k (keys sci/param->rname)]
      (is (= (sc-param-desc-as-map sc-from-xml k)
             (sc-param-desc-as-map sc-from-es k))))
    ))

(deftest test-validate-param-desc
  (let [scp-only-value (get sc-from-xml-params "exoscale-ch-gva.endpoint")
        scp-with-enum (get sc-from-xml-params "exoscale-ch-gva.orchestrator.instance.type")
        scp-with-instructions (get sc-from-xml-params "slipstream.mail.username")]
    (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-only-value))))
    (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-with-instructions))))
    (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-with-enum))))))
