(ns leihs.procurement.graphql.mutations
  (:require [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [admins :as admins]
             [budget-period :as budget-period]
             [budget-periods :as budget-periods] [categories :as categories]
             [main-categories :as main-categories] [request :as request]
             [requesters-organizations :as requesters-organizations]]))

; FIXME: a function for debugging convenience. will be a var later.
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
   :change-request-budget-period
     (fn [context args value]
       (let [rrequest (:request context)
             tx (:tx rrequest)
             auth-entity (:authenticated-entity rrequest)
             input-data (:input_data args)
             request (request/get-request-by-id tx (:id input-data))
             budget-period (budget-period/get-budget-period-by-id
                             tx
                             (:budget_period_id request))]
         (authorization/authorize-and-apply
           #(request/change-budget-period! context args value)
           :if-only
           #(and
              (not (budget-period/past? tx budget-period))
              (or (user-perms/admin? tx auth-entity)
                  (user-perms/inspector? tx auth-entity (:category_id request))
                  (and (user-perms/requester? tx auth-entity)
                       (request/requested-by? tx request auth-entity)
                       (budget-period/in-requesting-phase? tx
                                                           budget-period))))))),
   :change-request-category
     (fn [context args value]
       (let [rrequest (:request context)
             tx (:tx rrequest)
             auth-entity (:authenticated-entity rrequest)
             input-data (:input_data args)
             request (request/get-request-by-id tx (:id input-data))
             budget-period (budget-period/get-budget-period-by-id
                             tx
                             (:budget_period_id request))]
         (authorization/authorize-and-apply
           #(request/change-category! context args value)
           :if-only
           #(and
              (not (budget-period/past? tx budget-period))
              (or (user-perms/admin? tx auth-entity)
                  (user-perms/inspector? tx auth-entity (:category_id request))
                  (and (user-perms/requester? tx auth-entity)
                       (request/requested-by? tx request auth-entity)
                       (budget-period/in-requesting-phase? tx
                                                           budget-period))))))),
   ; FIXME: not complete! check permissions file!
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
   ; FIXME: not complete! check permissions file!
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