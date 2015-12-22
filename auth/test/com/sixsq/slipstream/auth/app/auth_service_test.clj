(ns com.sixsq.slipstream.auth.app.auth-service-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.app.auth-service :refer :all]))


(deftest test-parse-github-user

  (is (= {:login "st"
          :email "stephane.tavera@gmail.com"
          :name  "Stephane Tavera"}
         (parse-github-user
           "{\"login\":\"st\",\"id\":16004,\"avatar_url\":\"https://avatars.githubusercontent.com/u/16004?v=3\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/st\",\"html_url\":\"https://github.com/st\",\"followers_url\":\"https://api.github.com/users/st/followers\",\"following_url\":\"https://api.github.com/users/st/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/st/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/st/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/st/subscriptions\",\"organizations_url\":\"https://api.github.com/users/st/orgs\",\"repos_url\":\"https://api.github.com/users/st/repos\",\"events_url\":\"https://api.github.com/users/st/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/st/received_events\",\"type\":\"User\",\"site_admin\":false,\"name\":\"Stephane Tavera\",\"company\":null,\"blog\":null,\"location\":\"Geneva, Switzerland\",\"email\":\"stephane.tavera@gmail.com\",\"hireable\":null,\"bio\":null,\"public_repos\":9,\"public_gists\":10,\"followers\":6,\"following\":0,\"created_at\":\"2008-07-04T05:37:41Z\",\"updated_at\":\"2015-12-22T08:59:04Z\"}"
           ))))
