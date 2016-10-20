(ns com.sixsq.slipstream.tools.cli.utils
  (:require
    [clojure.string :as s]
    [clj-http.client :as http]
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
