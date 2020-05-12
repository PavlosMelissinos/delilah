(ns migrations
  (:require [migratus.core :as migratus]))


(def local {:connection-uri "jdbc:postgresql://localhost:5430/delilah"
            :classname      "org.postgresql.Driver"
            :host           "localhost"
            :user           "philistine"
            :password       "philistine-test"})

(defn config [db]
      {:store                :database
       :migrations-dir       "migrations/"
       :migration-table-name "migrations"
       :db                    db})



(defn init [db]
      (migratus/init (config db)))

(defn migrate [db]
      (migratus/migrate (config db)))

(defn rollback [db]
      (migratus/rollback (config db)))

(defn up [db id]
      (migratus/up (config db) id))

(defn down [db id]
      (migratus/down (config db) id))

(defn create [db name]
      (migratus/create (config db) name))

(defn pending [db]
      (migratus/pending-list (config db)))

(defn completed [db]
      (migratus/completed-list (config db)))
