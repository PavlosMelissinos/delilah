(ns philistine.db
  (:require [clojure.data.json :as json]
            [hugsql.core :as hugs]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc.connection :as connection]
            [clojure.java.io :as io])
    (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(defn init [connection]
  (connection/->pool ComboPooledDataSource connection))

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
