{:api-db                  {
                           :classname   "org.postgresql.Driver"
                           :subprotocol "postgresql"
                           :user        "postgres"
                           :password    "password"
                           :subname     "//localhost:5432/ssclj"
                           :make-pool?  true}
 :auth-db                 {
                           :classname   "org.postgresql.Driver"
                           :user        "postgres"
                           :password    "password"
                           :subprotocol "postgresql"
                           :subname     "//localhost:5432/slipstream"
                           :make-pool?  true}

 :token-nb-minutes-expiry 120
 :passphrase              "sl1pstre8m"

 :upstream-server         "http://localhost:8182"}

