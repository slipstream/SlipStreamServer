(ns slipstream.credcache.common
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [schema.core :as s]))

(def CommonAttributes
  {:id                           s/Str
   (s/optional-key :name)        s/Str
   (s/optional-key :description) s/Str
   :created                      s/Inst
   :updated                      s/Inst
   (s/optional-key :properties)  {s/Str s/Str}})

(def CredentialAttributes
  {:typeURI                 s/Str
   (s/optional-key :expiry) s/Int})

(def Credential
  (merge CommonAttributes CredentialAttributes))

(def MyProxyVomsCredential
  {:myproxy-host s/Str
   :myproxy-port s/Int
   :credential   s/Str})

(def VomsAttributes
  {s/Str {(s/optional-key :fqans)   [s/Str]
          (s/optional-key :targets) [s/Str]}})

(def CredentialTemplate
  (merge CommonAttributes
         {:myproxy-host s/Str
          :myproxy-port s/Int
          :username     s/Str
          :password     s/Str}))

(defn update-timestamps
  "Sets the :updated timestamp to the current date/time and will set
   the :created timestamp to the same time if it is not present in the
   input."
  [cred]
  (let [updated (t/now)
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


(defmulti template->resource
          "Converts a resource template to an instance of the resource.  This
           method dispatches on the :typeURI value in the template.  The default
           implementation will simply return the template as the resource, removing
           'Template' from the :typeURI value ignoring any fragment.  If :typeURI
           does not exist, then the default implementation will return the
           template unmodified."
          :typeURI)

(defmethod template->resource :default
           [{:keys [typeURI] :as template}]
  (if-let [resource-typeuri (get-resource-typeuri typeURI)]
    (assoc template :typeURI resource-typeuri)
    template))


(defmulti validate
          "Validates the given resource, returning the resource itself on success.
           This method dispatches on the value of :typeURI.  For any unknown
           :typeURI value the method throws an exception."
          :typeURI)

(defmethod validate :default
           [{:keys [typeURI] :as resource}]
  (throw (ex-info (str "unknown resource type: " typeURI) resource)))
