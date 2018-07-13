(ns leihs.procurement.schema
  (:require
    [clojure.java.io :as io]
    [clj-logging-config.log4j :as logging-config]
    [clojure.edn :as edn]
    [clojure.tools.logging :as log]
    [com.walmartlabs.lacinia.util :as graphql-util]
    [com.walmartlabs.lacinia.resolve :as graphql-resolve]
    [com.walmartlabs.lacinia.schema :as graphql-schema]
    [leihs.procurement.authorization :as authorization]
    [leihs.procurement.env :as env]
    [leihs.procurement.permissions.user :as user-perms]
    [leihs.procurement.resources.admins :as admins]
    [leihs.procurement.resources.building :as building]
    [leihs.procurement.resources.buildings :as buildings]
    [leihs.procurement.resources.budget-limits :as budget-limits]
    [leihs.procurement.resources.budget-period :as budget-period]
    [leihs.procurement.resources.budget-periods :as budget-periods]
    [leihs.procurement.resources.category :as category]
    [leihs.procurement.resources.categories :as categories]
    [leihs.procurement.resources.current-user :as current-user]
    [leihs.procurement.resources.inspectors :as inspectors]
    [leihs.procurement.resources.main-category :as main-category]
    [leihs.procurement.resources.main-categories :as main-categories]
    [leihs.procurement.resources.model :as model]
    [leihs.procurement.resources.organization :as organization]
    [leihs.procurement.resources.organizations :as organizations]
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.resources.requests :as requests]
    [leihs.procurement.resources.requesters-organizations :as
     requesters-organizations]
    [leihs.procurement.resources.room :as room]
    [leihs.procurement.resources.rooms :as rooms]
    [leihs.procurement.resources.settings :as settings]
    [leihs.procurement.resources.supplier :as supplier]
    [leihs.procurement.resources.user :as user]
    [leihs.procurement.resources.users :as users]
    [leihs.procurement.resources.viewers :as viewers]
    [leihs.procurement.utils.ring-exception :refer [get-cause]]
    [logbug.debug :as debug]))

(defn wrap-resolver-with-error
  [resolver]
  (fn [context args value]
    (try (resolver context args value)
         (catch Throwable _e
           (let [e (get-cause _e)
                 m (.getMessage e)
                 n (-> _e
                       .getClass
                       .getSimpleName)]
             (log/warn m)
             (if (env/env #{:dev :test}) (log/debug e))
             (graphql-resolve/resolve-as value {:message m, :exception n}))))))

; a function for debugging convenience. will be a var later.
(defn query-resolver-map
  []
  {:admins (-> admins/get-admins
               (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :budget-limits budget-limits/get-budget-limits,
   :budget-period budget-period/get-budget-period,
   :budget-periods budget-periods/get-budget-periods,
   :buildings buildings/get-buildings,
   :building building/get-building,
   :building-rooms rooms/get-building-rooms,
   :can-delete-budget-period? (-> budget-period/can-delete?
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin?])),
   :can-delete-category? (-> category/can-delete?
                             (authorization/wrap-ensure-one-of
                               [user-perms/admin?])),
   :can-delete-main-category? (-> main-category/can-delete?
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin?])),
   :category category/get-category,
   :categories categories/get-categories,
   :cost-center request/cost-center,
   :current-user current-user/get-current-user,
   :department organization/get-department,
   :general-ledger-account request/general-ledger-account,
   :inspectors inspectors/get-inspectors,
   :main-category main-category/get-main-category,
   :main-categories main-categories/get-main-categories,
   :model model/get-model,
   :organization organization/get-organization,
   :organizations organizations/get-organizations,
   :requests requests/get-requests,
   :requesters-organizations
     (-> requesters-organizations/get-requesters-organizations
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :procurement-account request/procurement-account,
   :room room/get-room,
   :rooms rooms/get-rooms,
   :settings (-> settings/get-settings
                 (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :supplier supplier/get-supplier,
   :total-price-cents-requested-quantities
     requests/total-price-cents-requested-quantities,
   :total-price-cents-approved-quantities
     requests/total-price-cents-approved-quantities,
   :total-price-cents-order-quantities
     requests/total-price-cents-order-quantities,
   :user user/get-user,
   :users users/get-users,
   :viewers (fn [context args value]
              (let [rrequest (:request context)
                    tx (:tx rrequest)
                    auth-entity (:authenticated-entity rrequest)]
                ((-> viewers/get-viewers
                     (authorization/wrap-ensure-one-of
                       [user-perms/admin?
                        (fn [tx auth-entity]
                          (user-perms/inspector? tx auth-entity (:id value)))])
                     context
                     args
                     value))))})

; a function for debugging convenience. will be a var later.
(defn mutation-resolver-map
  []
  {:create-request (fn [context args value]
                     (let [rrequest (:request context)
                           tx (:tx rrequest)
                           auth-entity (:authenticated-entity rrequest)
                           input-data (:input_data args)
                           budget-period (budget-period/get-budget-period-by-id
                                           tx
                                           (:budget_period input-data))]
                       (authorization/authorize-and-apply
                         #(request/create-request! context args value)
                         :if-only
                         #(and (not (budget-period/past? tx budget-period))
                               (or (user-perms/admin? tx auth-entity)
                                   (user-perms/inspector? tx auth-entity)
                                   (and (user-perms/requester? tx auth-entity)
                                        (= (:user input-data)
                                           (str (:user_id auth-entity)))
                                        (budget-period/in-requesting-phase?
                                          tx
                                          budget-period))))))),
   :change-request-category request/change-category!,
   :delete-request (fn [context args value]
                     ((-> request/delete-request!
                          (authorization/wrap-ensure-one-of
                            [user-perms/admin? user-perms/inspector?
                             (fn [tx auth-entity]
                               (and (user-perms/requester? tx auth-entity)
                                    (request/requested-by? tx
                                                           (:input_data args)
                                                           auth-entity)))]))
                       context
                       args
                       value)),
   :update-admins (-> admins/update-admins!
                      (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-budget-periods (-> budget-periods/update-budget-periods!
                              (authorization/wrap-ensure-one-of
                                [user-perms/admin?])),
   :update-categories-viewers (-> categories/update-categories-viewers!
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin? user-perms/inspector?])),
   :update-main-categories (-> main-categories/update-main-categories!
                               (authorization/wrap-ensure-one-of
                                 [user-perms/admin?])),
   :update-request (fn [context args value]
                     ((-> request/update-request!
                          (authorization/wrap-ensure-one-of
                            [user-perms/admin? user-perms/inspector?
                             (fn [tx auth-entity]
                               (and (user-perms/requester? tx auth-entity)
                                    (request/requested-by? tx
                                                           (:input_data args)
                                                           auth-entity)))]))
                       context
                       args
                       value)),
   :update-requesters-organizations
     (-> requesters-organizations/update-requesters-organizations!
         (authorization/wrap-ensure-one-of [user-perms/admin?]))})

(defn- wrap-map-with-error
  [arg]
  (into {} (for [[k v] arg] [k (wrap-resolver-with-error v)])))

(defn load-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (graphql-util/attach-resolvers (-> (query-resolver-map)
                                         (merge (mutation-resolver-map))
                                         wrap-map-with-error))
      graphql-schema/compile))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
