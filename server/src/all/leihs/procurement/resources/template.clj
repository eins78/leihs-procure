(ns leihs.procurement.resources.template
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.utils.sql :as sql]))

(defn insert-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_templates)
                     (sql/values [tmpl])
                     sql/format)))

(defn update-template!
  [tx tmpl]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_templates)
                     (sql/sset tmpl)
                     (sql/where [:= :procurement_templates.id (:id tmpl)])
                     sql/format)))

(defn delete-template!
  [tx id]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_templates)
                     (sql/where [:= :procurement_templates.id id])
                     sql/format)))

(defn get-template
  [tx tmpl]
  (let [where-clause (sql/map->where-clause :procurement_templates tmpl)]
    (-> (sql/select :procurement_templates.*)
        (sql/from :procurement_templates)
        (sql/merge-where where-clause)
        sql/format
        (->> (jdbc/query tx))
        first)))
