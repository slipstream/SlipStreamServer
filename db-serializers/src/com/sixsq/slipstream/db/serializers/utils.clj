(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [superstring.core :as s]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]))

(defn read-str
  [s]
  (try
    (read-string s)
    (catch RuntimeException ex
      (if-not (s/starts-with? (.getMessage ex) "Invalid token")
        (throw ex)
        s))))

(defn display
  [d & [msg]]
  (println msg)
  (clojure.pprint/pprint d)
  d)

;;
;; DB related.
;;

(defn set-es-client-uncond
  []
  (db/set-impl! (esb/get-instance)))

(defn set-es-client
  []
  (if (instance? clojure.lang.Var$Unbound db/*impl*)
    (set-es-client-uncond)))

(defn create-test-es-db-uncond
  []
  (esb/set-client! (esb/create-test-client)))

(defn create-test-es-db
  []
  (if (instance? clojure.lang.Var$Unbound esb/*client*)
    (create-test-es-db-uncond)))

(defn es-test-db-and-client
  []
  (set-es-client)
  (create-test-es-db))

(defn dump
  [resource & [msg]]
  (println "DB DUMP: " (or msg ""))
  (clojure.pprint/pprint (esu/dump esb/*client* esb/index-name resource)))

;;
;; Resource helpers.
;;

(defn as-request
  [body resource-uuid user-roles-str]
  (let [request {:params  {:uuid resource-uuid}
                 :body    (or body {})
                 :headers {aih/authn-info-header user-roles-str}}]
    ((aih/wrap-authn-info-header identity) request)))


