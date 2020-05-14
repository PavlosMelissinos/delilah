(ns philistine.db
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]

            [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugs]
            [integrant.core :as ig]
            [next.jdbc.connection :as connection])
    (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(defn init [connection]
  (connection/->pool ComboPooledDataSource connection))

(defmethod ig/init-key :db/connection
  [_ spec]
  (log/info :msg "Starting PostgreSQL connection pool..."
            :db-uri (:connection-uri spec))
  (connection/->pool ComboPooledDataSource spec))

(defmethod ig/halt-key! :db/connection
  [_ pool]
  (log/info :msg "Stopping PostgreSQL connection pool...")
  (.close pool))

;;;; Queries
(hugs/def-db-fns "philistine/queries.sql" {:adapter (next-adapter/hugsql-adapter-next-jdbc)})
(hugs/def-sqlvec-fns "philistine/queries.sql" {:adapter (next-adapter/hugsql-adapter-next-jdbc)})

(defn store-data! [db
                   {:keys [provider user] :as cfg}
                   {:keys [customer-code property-info bills] :as data}]
  (insert-account db {:username user
                      :provider (name provider)
                      :account-id customer-code})
  ;TODO: Setup json support -> https://cljdoc.org/d/seancorfield/next.jdbc/1.0.424/doc/getting-started/tips-tricks
  (insert-contract db {:account-id customer-code
                       :provider (name provider)
                       :contract-id (:contract-account property-info)
                       ;:data property-info
                       :data nil})
  (doseq [{:keys [bill-date] :as bill} bills]
    (insert-bill db {:contract-id (:contract-account property-info)
                     :provider (name provider)
                     :bill-date bill-date
                     :data nil})))
