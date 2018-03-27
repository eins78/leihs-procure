(ns procurement-graphql.permissions.request
  (:require [procurement-graphql.resources.request :as r]
            [procurement-graphql.resources.budget-period :as bp]
            [procurement-graphql.resources.user :as u]))  

; (def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")
; (def request-id "91805c8c-0f47-45f1-bcce-b11da5427294")

; (def test-user (u/get-user user-id))
; (def test-request (r/get-request request-id))

(defn edit? [user request]
  (let [budget_period (bp/get-budget-period (:budget_period_id request))]
    (and (u/procurement-requester? user)
         (r/requested-by-user? request user)
         (bp/in-requesting-phase? budget_period))))

; (edit? test-user test-request)
