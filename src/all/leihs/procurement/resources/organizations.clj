(ns leihs.procurement.resources.organizations
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as logging]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]
            ))

(def organizations-base-query
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)))

(defn organizations-query [_ args value]
  (let [root-only (:root_only args)
        id (:id value)]
    (sql/format
      (cond-> organizations-base-query
        root-only
        (sql/merge-where [:=
                          :procurement_organizations.parent_id
                          nil])
        id
        (sql/merge-where [:=
                          :procurement_organizations.parent_id
                          id])))))

(defn get-organizations [context args value]
  (jdbc/query (-> context :request :tx)
              (organizations-query context args value)))

(defn delete-unused [tx]
  "first delete organizations (parent_id IS NOT NULL) without requesters
  and requests, then delete departments (parent_id IS NULL) without children"
  (jdbc/execute!
    tx (-> (sql/delete-from [:procurement_organizations :po])
           (sql/merge-where [:<> :po.parent_id nil])
           (sql/merge-where
             [:not (sql/call
                     :exists
                     (-> (sql/select 1)
                         (sql/from [:procurement_requesters_organizations :pro])
                         (sql/where [:= :pro.organization_id :po.id])))])
           (sql/merge-where
             [:not (sql/call
                     :exists
                     (-> (sql/select 1)
                         (sql/from [:procurement_requests :pr])
                         (sql/where [:= :pr.organization_id :po.id])))])
           sql/format))
  (jdbc/execute!
    tx (-> (sql/delete-from [:procurement_organizations :po1])
	   (sql/merge-where [:= :po1.parent_id nil])
	   (sql/merge-where
	     [:not (sql/call
		     :exists
		     (-> (sql/select 1)
			 (sql/from [:procurement_organizations :po2])
			 (sql/where [:= :po2.parent_id :po1.id])))])
	   sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
