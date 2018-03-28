(ns leihs.procurement.resources.user
  (:require [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [clojure.java.jdbc :as jdbc]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")

(defn user-query [id]
  (-> (sql/select :*)
      (sql/from :users)
      (sql/where [:= :users.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-user [id]
  (first (jdbc/query (get-ds) (user-query id))))

(defn procurement-requester? [user]
  (:is_procurement_requester user))

(defn procurement-admin? [user]
  (:is_procurement_admin user))

(defn procurement-inspector? [user]
  (:result
    (jdbc/query
      (get-ds)
      (-> (sql/select
            [(sql/call
               :exists
               (-> (sql/select 1)
                   (sql/from :procurement_category_inspectors)
                   (sql/where [:=
                               :procurement_category_inspectors.user_id
                               (:id user)])))
             :result])
          sql/format))))

; (procurement-inspector? (get-user user-id))
