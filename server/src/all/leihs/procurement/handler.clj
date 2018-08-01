(ns leihs.procurement.handler
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]])
  (:require
    [leihs.procurement.anti-csrf.core :as anti-csrf]
    [leihs.procurement.authorization :refer [wrap-authorize]]
    [leihs.procurement.backend.html :as html]
    [leihs.procurement.constants :as constants]
    [leihs.procurement.env :as env]
    [leihs.procurement.resources.upload :as upload]
    [leihs.procurement.graphql :as graphql]
    [leihs.procurement.auth.session :as session]
    [leihs.procurement.paths :refer [path paths]]
    [leihs.procurement.resources.attachment :as attachment]
    [leihs.procurement.resources.image :as image]
    [leihs.procurement.shutdown :as shutdown]
    [leihs.procurement.status :as status]
    [leihs.procurement.utils.ds :as ds]
    [leihs.procurement.utils.http-resources-cache-buster :as cache-buster :refer
     [wrap-resource]]
    [leihs.procurement.utils.json-protocol]
    [leihs.procurement.utils.ring-exception :as ring-exception]
    [bidi.bidi :as bidi]
    [bidi.ring :refer [make-handler]]
    [cheshire.core :as json]
    [compojure.core :as cpj]
    [ring.middleware.accept]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.util.response :refer [redirect]]
    [ring-graphql-ui.core :refer [wrap-graphiql]]
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as log]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I>]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]))

(declare redirect-to-root-handler)

; ========================================================
; TODO: remove shutdown!!! (and possible others)
(def skip-authorization-handler-keys
  #{:attachment :upload :image :shutdown :status})
; ========================================================

(def handler-resolve-table
  {:attachment attachment/routes,
   :upload upload/routes,
   :graphql graphql/handler,
   :image image/routes,
   :not-found html/not-found-handler,
   :shutdown shutdown/routes,
   :status status/routes})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request] (redirect (path :root)))

(defn handler-resolver
  [handler-key]
  (get handler-resolve-table handler-key nil))

(defn dispatch-to-handler
  [request]
  (if-let [handler (:handler request)]
    (handler request)
    (throw
      (ex-info
        "There is no handler for this resource and the accepted content type."
        {:status 404, :uri (get request :uri)}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-resolve-handler
  ([handler] (fn [request] (wrap-resolve-handler handler request)))
  ([handler request]
   (let [path (or (-> request
                      :path-info
                      presence)
                  (-> request
                      :uri
                      presence))
         {route-params :route-params, handler-key :handler}
           (bidi/match-pair paths {:remainder path, :route paths})
         handler-fn (handler-resolver handler-key)]
     (handler (assoc request
                :route-params route-params
                :handler-key handler-key
                :handler handler-fn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canonicalize-params-map
  [params]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]] [(keyword k)
                           (try (json/parse-string v true)
                                (catch Exception _ v))]))
         (into {}))))

(defn wrap-canonicalize-params-maps
  [handler]
  (fn [request]
    (handler (-> request
                 (update-in [:params] canonicalize-params-map)
                 (update-in [:query-params] canonicalize-params-map)
                 (update-in [:form-params] canonicalize-params-map)))))

(defn wrap-empty [handler] (fn [request] (or (handler request) {:status 404})))

(defn wrap-secret-byte-array
  "Adds the secret into the request as a byte-array (to prevent
  visibility in logs etc) under the :secret-byte-array key."
  [handler secret]
  (fn [request] (handler (assoc request :secret-ba (.getBytes secret)))))

(defn wrap-reload-if-dev-or-test
  [handler]
  (cond-> handler
    (#{:dev :test} env/env) (wrap-reload {:dirs ["src" "resources"]})))

(defn init
  [secret]
  (-> dispatch-to-handler
      wrap-json-response
      (wrap-json-body {:keywords? true})
      ; anti-csrf/wrap
      (wrap-authorize skip-authorization-handler-keys)
      session/wrap
      wrap-cookies
      wrap-empty
      (wrap-secret-byte-array secret)
      ; ===================================================================
      ; NOTE: this two wrappers have to be in this order as the first
      ; depends on the second (context: hanging of requests due to images)
      ; ds/wrap-tx
      ds/wrap-cheat-tx
      wrap-resolve-handler
      ; ===================================================================
      (wrap-graphiql {:path "/procure/graphiql", :endpoint "/procure/graphql"})
      wrap-canonicalize-params-maps
      wrap-params
      wrap-multipart-params
      ring-exception/wrap
      wrap-reload-if-dev-or-test))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
