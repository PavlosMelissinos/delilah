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
                   {:keys [provider user pass] :as cfg}
                   {:keys [customer-code property-info] :as data}]
  (insert-account db {:username user
                      :password pass
                      :provider provider
                      :account-id customer-code})
  ;TODO: Setup json support -> https://cljdoc.org/d/seancorfield/next.jdbc/1.0.424/doc/getting-started/tips-tricks
  (insert-contract db {:account-id customer-code
                       :provider provider
                       :contract-id (:contract-account property-info)
                       ;:data property-info
                       :data nil})
  #_(insert-bill db {}))

(comment
  ; Migrations, TODO: replace with migratus
  "migrations.sql")
