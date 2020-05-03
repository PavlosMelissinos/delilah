(ns philistine.db
  (:require [hugsql.core              :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc.connection     :as connection])
    (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(defn init [connection]
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))
  (connection/->pool ComboPooledDataSource connection))
