(ns com.sixsq.slipstream.ssclj.resources.spec.util.spec-tools

  (:require [clojure.spec.alpha :as s]
            [spec-tools.json-schema :as jsc]
            [clojure.walk :as w]
            [com.sixsq.slipstream.ssclj.resources.spec.util.es-tools :as et]
            [com.sixsq.slipstream.ssclj.resources.spec.metering]

            )
  )

(defn remove-array-schema [x]
  (if (s/valid? :es/array-map x)
    (:items x)
    x))

(defn remove-required-key [x]
  (if (:required x)
    (dissoc x :required)
    x)
  )

(defn deprecated-string-type [x]
  (if (s/valid? :es/type-string x)
    (assoc x :type "keyword")
    x
    )
  )


(defn transform [m]
  (->> m
       (w/postwalk #(remove-array-schema %))
       (w/postwalk #(remove-required-key %))
       (w/postwalk #(deprecated-string-type %))
       ))

(defn spec->es-mapping
  [spec]
  (let [json-map (-> spec
                     jsc/transform
                     :properties
                     transform
                     )
        keywordized-map (into {}
                              (for [[k v] json-map]
                                [(keyword k) v]))
        map-without-nils (into {} (filter second keywordized-map))
        ]

    {:mappings {:_doc {:properties map-without-nils}}}
    ))

(s/def :es/type-array #(= "array" (:type %)))
(s/def :es/type-string #(= "string" (:type %)))
(s/def :es/items-map #(-> %
                          :items
                          :type
                          string?
                          ))

(s/def :es/array-map (s/and :es/type-array :es/items-map))









