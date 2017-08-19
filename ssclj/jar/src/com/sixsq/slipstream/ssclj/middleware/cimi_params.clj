(ns com.sixsq.slipstream.ssclj.middleware.cimi-params
  (:require
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.impl :as impl]))

(defn wrap-cimi-params
  "Provides a middleware wrapper that adds a map containing the values of the
  provided CIMI query parameters to the request. This map is added to the
  :cimi-params key in the request.

  The response map is not modified by this middleware.

  All of the following keys will be present in the modified request, although
  they may be nil if they were not specified.

    - :first      value of the $first parameter, nil if invalid or
                  not specified
    - :last       value of the $last parameter, nil if invalid or
                  not specified
    - :filter     contains the AST for the valid filters in hiccup
                  format. If multiple filters are provided, they are
                  combined with a logical AND.  This will be nil if
                  the parameter was not specified or was invalid.
    - :select     contains a set of the attributes the client has
                  selected.  Will be nil if unspecified or if the
                  user specified the wildcard '*'.
    - :expand     indicates the reference attributes to expand in the
                  response: :none if unspecified, :all if the wildcard
                  was used, or an explicit set of attributes.
    - :orderby    nil if unspecified or no valid values were provided.
                  Otherwise, provides an ordered vector of attribute,
                  direction pairs.  The direction is either :asc
                  (ascending) or :desc (descending).

  This middleware also processes the following parameters that extend the CIMI
  specification.

    - :size       the maximum number of documents that will be returned
                  by a database query.  This is independent of the paging
                  parameters.  Setting this to zero allows queries in
                  which only aggregated information is returned.
    - :metric     provides a map where the key is the aggregation algorithm
                  and the value is a vector of parameter names to which the
                  aggregation applies.  This is specified by parameter
                  names and values like: '$metric=keyname:algo'.  The
                  ':algo' suffix names the algorithm and is required.
                  Examples are ':sum', ':min', and ':max'.

  Middleware that processes CIMI parameters from the :params map in the
  request. Because this uses the :params map, the wrap-params middleware
  **must** be run prior to this wrapper in the ring processing chain."
  [handler]
  (fn [{:keys [params] :as req}]
    (let [cimi-params (assoc {}
                        :first (impl/cimi-first params)
                        :last (impl/cimi-last params)
                        :filter (impl/cimi-filter params)
                        :expand (impl/cimi-expand params)
                        :select (impl/cimi-select params)
                        :format (impl/cimi-format params)
                        :orderby (impl/cimi-orderby params)
                        :size (impl/cimi-size params)
                        :metric (impl/cimi-metric params))]
      (handler (assoc req :cimi-params cimi-params)))))
