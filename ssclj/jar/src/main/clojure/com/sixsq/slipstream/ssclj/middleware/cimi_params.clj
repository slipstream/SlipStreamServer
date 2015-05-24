(ns com.sixsq.slipstream.ssclj.middleware.cimi-params
  "Provides a middleware wrapper that adds a map containing the
  values of the provided CIMI query parameters to the request.
  This map is added to the :cimi-params key in the request.

  The response map is not modified by this middleware.

  All of the following keys will be present in the modified request,
  although they may be nil if they were not specified.

    - :first -   value of the $first parameter, nil if invalid or
                 not specified
    - :last -    value of the $last parameter, nil if invalid or
                 not specified
    - :filter -  contains the AST for the valid filters in hiccup
                 format. If multiple filters are provided, they are
                 combined with a logical AND.  This will be nil if
                 the parameter was not specified or was invalid.
    - :select -  contains a set of the attributes the client has
                 selected.  Will be nil if unspecified or if the
                 user specified the wildcard '*'.
    - :expand -  indicates the reference attributes to expand in the
                 response: :none if unspecified, :all if the wildcard
                 was used, or an explicit set of attributes.
    - :orderby - nil if unspecified or no valid values were provided.
                 Otherwise, provides an ordered vector of attribute,
                 direction pairs.  The direction is either :asc
                 (ascending) or :desc (descending)."

  (:require
    [com.sixsq.slipstream.ssclj.middleware.cimi-params-impl :as u]))

(defn wrap-cimi-params
  "Middleware that processes CIMI parameters from the :params map
   in the request.  Because this uses the :params map, the wrap-params
   middleware **must** be run prior to this wrapper in the ring
   processing chain."
  [handler]
  (fn [req]
    (->> req
         (u/process-first-last)
         (u/process-filter)
         (u/process-expand)
         (u/process-select)
         (u/process-format)
         (u/process-orderby)
         (handler))))
