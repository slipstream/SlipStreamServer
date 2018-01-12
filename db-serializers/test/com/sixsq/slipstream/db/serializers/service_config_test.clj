(ns com.sixsq.slipstream.db.serializers.service-config-test
  (:require
    [clojure.java.io :as io]
    [clojure.set :as cs]
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [me.raynes.fs :as fs]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.dbtest.es.utils-esdb :as ud]
    [com.sixsq.slipstream.ssclj.resources.spec.description]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream :as crtpls] ;; template and resource have same schema
    [com.sixsq.slipstream.ssclj.util.config :as ssclj-cu])
  (:import
    (com.sixsq.slipstream.persistence ServiceConfiguration)))

(def sc-from-xml (-> "configuration.xml"
                     io/resource
                     .getPath
                     slurp
                     scu/conf-xml->sc))

(def sc-from-xml-params (.getParameters sc-from-xml))

(defn- keys-outside
  [conf]
  (cs/difference (set (keys sci/rname->param)) (set (keys conf))))

(defn msg-keys-not-in-cfg
  [keys]
  (str "The following keys are not in config " (clojure.string/join ", " keys)))

(defn sc-param-desc-as-map
  [sc k]
  (let [p (.getParameter sc k)]
    (u/desc-from-param p)))


;; Fixtures
(use-fixtures :once ud/test-fixture-es-client-and-db-impl)

;; initialize resource (including possible connectors on the classpath).
(u/initialize)

;; Tests

;; Test transformation from ServiceConfiguration to
;; configuration/slipstream resource document.
(deftest test-sc->cfg
  (is (= {} (dissoc (sci/sc->cfg (ServiceConfiguration.)) :id)))
  (let [conf (sci/sc->cfg sc-from-xml)
        not-in-conf (keys-outside conf)]
    (is (empty? not-in-conf) (msg-keys-not-in-cfg not-in-conf))))

;; Test schema compliance after transformation of SC to
;; configuration/slipstream resource document.
(deftest test-check-sc-schema
  (let [cfg (sci/complete-resource (sci/sc->cfg sc-from-xml))]
    (is (s/valid? :cimi/configuration-template.slipstream cfg))))


(deftest test-attrs-map-valid
  (let [config-attrs (set (map (comp keyword name) (:req-un crtpls/configuration-template-keys-spec-req)))]
    (is (= (set (keys sci/rname->param))
           config-attrs))))

(deftest test-fail-store-if-no-default-in-db
  (is (thrown? RuntimeException (sci/store-sc (ServiceConfiguration.)))))


(deftest test-save-load-values-ok
  (sci/db-add-default-config)
  (let [sc-to-es (sci/store-sc sc-from-xml)
        sc-from-es (sci/load-sc)]
    (is (not (nil? sc-from-es)))
    (doseq [k (keys sci/param->rname)]
      (is (= (scu/sc-get-param-value sc-to-es k)
             (scu/sc-get-param-value sc-from-es k))))))


(def conf-desc-file (str "test-resources/" (fs/temp-name "conf-desc-" ".edn")))

(deftest test-save-load-desc-ok
  (try
    (sci/db-add-default-config)
    (sci/cs->cfg-desc-and-spit sc-from-xml conf-desc-file)
    (sci/store-sc sc-from-xml)
    (let [sc-from-es (with-redefs-fn {#'com.sixsq.slipstream.db.serializers.service-config-impl/cfg-desc
                                      (ssclj-cu/read-config conf-desc-file)}
                       #(sci/load-sc))]
      (doseq [k (keys sci/param->rname)]
        (is (= (sc-param-desc-as-map sc-from-xml k)
               (sc-param-desc-as-map sc-from-es k)))))
    (finally
      (fs/delete conf-desc-file))))


(deftest test-validate-param-desc
  (doseq [desc (map (partial get sc-from-xml-params) ["exoscale-ch-gva.endpoint"
                                                      "exoscale-ch-gva.orchestrator.instance.type"
                                                      "slipstream.mail.username"])]
    (is (s/valid? :cimi.desc/parameter-description (u/desc-from-param desc)))))
