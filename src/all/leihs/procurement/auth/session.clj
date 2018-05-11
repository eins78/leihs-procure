(ns leihs.procurement.auth.session
  (:require
    [cider-ci.open-session.encryptor :as encryptor :refer [decrypt encrypt]]
    [leihs.procurement.constants :refer [USER_SESSION_COOKIE_NAME]]
    [leihs.procurement.utils.ds :as ds]
    [leihs.procurement.utils.ring-exception :as ring-exception]
    [leihs.procurement.utils.sql :as sql]

    [clojure.java.jdbc :as jdbc]
    [clojure.walk :refer [keywordize-keys]]
    [pandect.core]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    )
  (:import
    [java.util UUID]
    ))

(defn delete-session-cookie [response]
  (assoc-in response [:cookies (str USER_SESSION_COOKIE_NAME)]
            {:value "" :path "/" :max-age 0}))

(defn session-error-page [exception request]
  (-> {:status 401
       :body (str "Session authentication error: "
                  (.getMessage exception))}
      delete-session-cookie))

(defn user-with-valid-session-query [token]
  (-> (sql/select [:users.id :user_id]
                  :is_admin :sign_in_enabled
                  :firstname :lastname :email
                  [:user_sessions.id :user_session_id]
                  [:user_sessions.created_at :user_session_created_at])
      (sql/from :users)
      (sql/merge-join :user_sessions [:= :users.id :user_id])
      (sql/merge-join :settings [:= :settings.id 0])
      (sql/merge-where (sql/call
                         := :user_sessions.token_hash
                         (sql/call :encode
                                   (sql/call :digest token "sha256")
                                   "hex")))
      (sql/merge-where
        (sql/raw (str "now() < user_sessions.created_at + "
                      " settings.sessions_max_lifetime_secs * interval '1 second'")))
      ; (sql/merge-where [:= :sign_in_enabled true])
      sql/format))

(defn user-auth-entity! [token]
  (if-let [uae (->> (user-with-valid-session-query token)
                    (jdbc/query (ds/get-ds)) first)]
    (assoc uae
           :authentication-method :session
           :scope_read true
           :scope_write true
           :scope_admin_read (:is_admin uae)
           :scope_admin_write (:is_admin uae))
    (throw (IllegalStateException. (str "No valid user session found!")))))

(defn session-cookie-value [request]
  (when-let [cookie (-> request :cookies
                        (get USER_SESSION_COOKIE_NAME nil) :value)]
    (decrypt (-> request :secret-ba String.) cookie)))

; calling the handler must never thrown an error but return a fail response
(defn authenticate [request _handler]
  (catcher/snatch
    {:level :warn
     :return-fn (fn [e] (session-error-page e request))}
    (let [handler (ring-exception/wrap _handler)]
      (if-let [cookie-value (session-cookie-value request)]
        (let [user-auth-entity (-> cookie-value :token user-auth-entity!)]
          (handler
            (assoc request :authenticated-entity user-auth-entity)))
        (handler request)))))

(defn wrap [handler]
  (fn [request]
    (authenticate request handler)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns 'cider-ci.open-session.encryptor)
; (debug/debug-ns *ns*)

