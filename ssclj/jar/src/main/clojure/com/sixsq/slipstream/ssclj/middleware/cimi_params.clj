(ns com.sixsq.slipstream.ssclj.middleware.cimi-params
  (:require
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [clojure.string :as s]))

;;
;; List of mime-type abbreviations and the associated full mime-type name.
;; All of the mime-type abbreviations must be in lowercase.
;;
(def accepted-mime-types {"json" "application/json",
                          "xml" "application/xml",
                          "edn" "application/edn"})

(def accepted-formats (set (keys accepted-mime-types)))

(defn as-vector
  "Ensures that the given argument is a vector, coercing the
   given value if necessary.  Vectors and lists are returned
   as a vector.  Nil returns an empty list.  All other values
   will be wrapped into a 1-element vector."
  [arg]
  (cond
    (nil? arg) []
    (vector? arg) arg
    (list? arg) (vec arg)
    :else [arg]))

(defn as-long
  "Coerse the value into a long.  The value can either be a
   string or a long."
  [s]
  (let [s (str s)]
    (try
      (Long/parseLong ^String s)
      (catch NumberFormatException _
        nil))))

(defn first-valid-long
  "In a vector of strings or numbers, this extracts the first
   value that can be coerced into a valid long."
  [v]
  (->> v
       (as-vector)
       (map as-long)
       (remove nil?)
       (first)))

(defn get-index
  [m k]
  (->> (get m k)
       (first-valid-long)))

(defn process-first-last
  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> ["$first" "$last"]
       (map #(get-index params %))
       (zipmap [:first :last])
       (merge cimi-params)
       (assoc req :cimi-params)))

(defn conjunction
  "TODO!!"
  ([m]
    m)
  ([a m]
    m))

(defn process-filter
  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> (get params "$filter")
       (as-vector)
       (map parser/parse-cimi-filter)
       (reduce conjunction)
       (assoc cimi-params :filter)
       (assoc req :cimi-params)))

(defn comma-split
  [s]
  )

(defn process-expand
  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> (get params "$expand")
       (as-vector)
       (mapcat comma-split)
       (set)
       (assoc cimi-params :expand)
       (assoc req :cimi-params)))

(defn process-select
  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> (get params "$select")
       (as-vector)
       (mapcat comma-split)
       (set)
       (concat #{"resourceURI"})
       (assoc cimi-params :select)
       (assoc req :cimi-params)))

;;
;;
(defn process-format
  "Processes the $filter parameter(s) and adds the requested mime type to
  the :cimi-params map under the :format key.  The processing of the
  $filter parameter is more lenient than the CIMI specification in the
  following ways:

    - The first _acceptable_ value is used, rather than strictly the first.
    - Surrounding whitespace is removed from values before processsing.

  In addition to the 'json' and 'xml' values in the specificiation, this
  also accepts 'edn'."

  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> (get params "$format")
       (as-vector)
       (filter string?)
       (map s/trim)
       (map s/lower-case)
       (filter accepted-formats)
       (map #(get accepted-mime-types %))
       (first)
       (assoc cimi-params :format)
       (assoc req :cimi-params)))

(defn orderby-clause [e]
  e)

(defn process-orderby
  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> (get params "$orderby")
       (as-vector)
       (mapcat comma-split)
       (map orderby-clause)
       (assoc cimi-params :orderby)
       (assoc req :cimi-params)))

(defn wrap-cimi-params
  "Middleware that processes CIMI parameters from the :params map
   in the request.  Because this uses the :params map, the wrap-params
   middleware must be run before in the processing chain."
  [handler]
  (fn [request]
    (->> (assoc request :cimi-params {})
         (process-first-last)
         (process-filter)
         (process-expand)
         (process-select)
         (process-format)
         (process-orderby)
         (handler))))
