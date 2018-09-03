(ns leihs.procurement.auth.session
  (:require [leihs.procurement.resources.user :as u]
            [clojure.tools.logging :as log]))

(defn wrap
  [handler]
  (fn [request]
    (let [user-id-from-cookie (-> request
                                  :cookies
                                  (get "leihs-fake-cookie-auth" nil)
                                  :value)
          user-id-from-token (some-> request
                                     :headers
                                     (get "leihs-fake-token-auth")
                                     (->> (u/get-user-by-id (:tx request)))
                                     :id)]
      (if user-id-from-cookie
        (log/debug "[>- leihs-fake-cookie-auth]" user-id-from-cookie))
      (if user-id-from-token
        (log/debug "[>- leihs-fake-token-auth]" user-id-from-token))
      (handler (assoc request
                 :authenticated-entity {:user_id (or user-id-from-cookie
                                                     user-id-from-token)})))))
