(ns com.sixsq.slipstream.tools.cli.utils
  (:require
    [clojure.string :as s]
    [clojure.edn :as edn]
    [clj-http.client :as http]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]
    ))

(defn- update-val
  [v modifiers]
  (let [nvs (for [[m r] modifiers :when (and (string? v) (re-find m v))]
              (s/replace v m r))]
    (if (seq nvs)
      (last nvs)
      v)))

(defn modify-vals
  [con modifiers]
  (let [res (for [[k v] con]
              [k (update-val v modifiers)])]
    (into {} res)))

(def con-attrs-to-remove
  {"ec2"      [:securityGroup]
   "nuvlabox" [:orchestratorInstanceType :pdiskEndpoint]})

(defn remove-attrs
  "Cleanup of old attributes."
  [con]
  (if-let [attrs (get con-attrs-to-remove (:cloudServiceType con))]
    (apply dissoc con attrs)
    con))

(defn can-validate-attr-type?
  [k]
  (contains? sci/connector-ref-attrs-defaults k))

(defn- types-differ?
  "Contains in the attributes list and types differ."
  [k v]
  (not= (type v) (type (k sci/connector-ref-attrs-defaults))))

(defn change-connector-val-type
  [k v]
  (if (can-validate-attr-type? k)
    (if (types-differ? k v)
      (let [attr-type (type (k sci/connector-ref-attrs-defaults))]
        (cond
          (= java.lang.String attr-type) [k (str v)]
          (= java.lang.Long attr-type) [k (read-string v)]
          (= java.lang.Boolean attr-type) [k (read-string v)]
          :else [k v]))
      [k v])
    (if (= java.lang.Boolean (type v))
      [k v]
      [k (str v)])))

(defn change-connector-vals-types
  [con]
  (into {} (map #(apply change-connector-val-type %) con)))

(defn ->config-resource
  [url]
  (str url "/configuration"))

(defn conf-xml
  [path-url creds]
  (if (s/starts-with? path-url "https")
    (-> path-url
        ->config-resource
        (http/get {:follow-redirects false
                   :accept           :xml
                   :basic-auth       creds})
        :body)
    (slurp path-url)))

(defn cfg-path-url->sc
  [cfg-path-url creds]
  (-> cfg-path-url
      (conf-xml creds)
      scu/conf-xml->sc))

(defn slurp-edn
  [f]
  (edn/read-string (slurp f)))

(defn spit-edn
  [obj f]
  (scu/spit-pprint obj f))
;;
;; Command line options processing.
;;

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn error-msg
  [& errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn cli-parse-sets
  ([m k v]
   (cli-parse-sets m k v identity))
  ([m k v fun] (assoc m k (if-let [oldval (get m k)]
                            (merge oldval (fun v))
                            (hash-set (fun v))))))

(defn cli-parse-connectors
  [m k v]
  (cli-parse-sets m k v))

(defn ->re-match-replace
  "'m=r' -> [#'m' 'r']"
  [mr]
  (let [m-r (s/split mr #"=")]
    [(re-pattern (first m-r)) (second m-r)]))

(defn cli-parse-modifiers
  [m k v]
  (cli-parse-sets m k v ->re-match-replace))

