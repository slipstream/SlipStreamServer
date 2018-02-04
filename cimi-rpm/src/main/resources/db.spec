{:token-nb-minutes-expiry 10080

 ;; Used by front server when redirecting (URL accessed only locally)
 :upstream-server         "http://localhost:8182"

 ;; The following properties require adaptations

 ;; Used by external authentication providers
 ;; auth-server and main-server typically contain same value
 ;; when nginx is the front proxy server
 :auth-server             "SlipStream URL"
 :main-server             "SlipStream URL"

 ;; Application must be registered on Github
 ;; See https://github.com/settings/applications/new
 ;; Homepage URL can be <SlipStream end point>
 ;; The Authorization callback URL must be <SlipStream end point>/api/session/

 :github-client-id        "Github 'Client ID'"
 :github-client-secret    "Github 'Client Secret'"}

