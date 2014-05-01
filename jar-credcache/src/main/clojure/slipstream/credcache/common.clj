(ns slipstream.credcache.common
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [clj-time.format :as tfmt]
    [schema.core :as s]))

;;
;; common schema definitions
;;

(def CommonTemplateAttributes
  {:typeURI                      s/Str
   (s/optional-key :name)        s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :properties)  {s/Str s/Str}})

(def CommonResourceAttributes
  (merge CommonTemplateAttributes
         {:id      s/Str
          :created s/Str
          :updated s/Str}))

;;
;; common resource utilities and multimethods
;;

(defn update-timestamps
  "Sets the :updated timestamp to the current date/time and will set
   the :created timestamp to the same time if it is not present in the
   input."
  [cred]
  (let [updated (tfmt/unparse (:date-time tfmt/formatters) (t/now))
        created (get cred :created updated)]
    (assoc cred :created created :updated updated)))

(defn get-resource-typeuri
  "This will return the resource typeURI corresponding to the given
   resource template typeURI by removing 'Template' at the end of the
   URI.  Returns modified value or nil if nil was passed in."
  [template-typeuri]
  (if template-typeuri
    (->> template-typeuri
         (re-matches #"^(.*?)(?:Template)?(#.*)?$")
         (rest)
         (apply str))))

(defn update-resource-typeuri
  [template]
  (let [template-typeuri (:typeURI template)
        resource-typeuri (get-resource-typeuri template-typeuri)]
    (assoc template :typeURI resource-typeuri)))

(defn resource-type-dispatch
  [{:keys [typeURI subtypeURI]}]
  [typeURI subtypeURI])

(defmulti template->resource
          "Converts a resource template to an instance of the resource.  This
           dispatches on the value [:typeURI :subtypeURI].  The default implementation
           copies the template with a modified :typeURI field.  The method removes
           'Template' from the end of the original :typeURI value."
          resource-type-dispatch)

(defmethod template->resource :default
           [{:keys [typeURI] :as template}]
  (if-let [resource-typeuri (get-resource-typeuri typeURI)]
    (assoc template :typeURI resource-typeuri)
    template))


(defmulti validate-template
          "Validates the given resource template, returning the template itself on success.
           This method dispatches on the value of [:typeURI :subtypeURI].  For any unknown
           dispatch value the method throws an exception."
          resource-type-dispatch)

(defmethod validate-template :default
           [template]
  (throw (ex-info (str "unknown resource template type: " (resource-type-dispatch template)) template)))


(defmulti validate
          "Validates the given resource, returning the resource itself on success.
           This method dispatches on the value of [:typeURI :subtypeURI].  For any unknown
           dispatch value the method throws an exception."
          resource-type-dispatch)

(defmethod validate :default
           [resource]
  (throw (ex-info (str "unknown resource type: " (resource-type-dispatch resource)) resource)))
