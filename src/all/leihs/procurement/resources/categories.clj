(ns leihs.procurement.resources.categories
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.category :as category]
            [leihs.procurement.resources.inspectors :as inspectors]
            [leihs.procurement.resources.viewers :as viewers]
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

(defn delete-categories-not-in-main-category-ids! [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_categories)
                     (sql/where [:not-in :procurement_categories.main_category_id ids])
                     sql/format)))

(defn delete-categories-for-main-category-id-and-not-in-ids! [tx mc-id ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_categories)
                     (sql/merge-where [:= :procurement_categories.main_category_id mc-id])
                     (cond-> (not (empty? ids))
                       (sql/merge-where [:not-in :procurement_categories.id ids]))
                     sql/format
                     )))

(defn update-categories! [tx mc-id cs]
  (loop [[c & rest-cs] cs c-ids []]
    (if c 
      (do (if (:id c)
            (category/update-category! tx (dissoc c :inspectors :viewers))
            (category/insert-category! tx (dissoc c :id :inspectors :viewers)))
          (let [c-id (or (:id c)
                         (as-> c <>
                           (select-keys <> [:name :main_category_id])
                           (category/get-category tx <>)
                           (:id <>)))]
            (inspectors/update-inspectors! tx c-id (:inspectors c))
            (viewers/update-viewers! tx c-id (:viewers c))
            (recur rest-cs (conj c-ids c-id))))
      (delete-categories-for-main-category-id-and-not-in-ids! tx mc-id c-ids))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
