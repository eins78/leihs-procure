(ns leihs.procurement.resources.categories
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def categories-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn categories-query [context arguments value]
  (let [inspected-by-auth-user (:inspected_by_auth_user arguments)
        main-category-id (:main_category_id value)]
    (sql/format
      (cond-> categories-base-query
        main-category-id
        (sql/merge-where [:=
                          :procurement_categories.main_category_id
                          main-category-id])
        inspected-by-auth-user
        (->
          (sql/merge-join :procurement_category_inspectors
                          [:=
                           :procurement_category_inspectors.category_id
                           :procurement_categories.id])
          (sql/merge-where
            [:=
             :procurement_category_inspectors.user_id
             (-> context :request :authenticated-entity :id)]))))))

(defn get-categories [context arguments _]
  (jdbc/query (-> context :request :tx)
              (categories-query context arguments _)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
