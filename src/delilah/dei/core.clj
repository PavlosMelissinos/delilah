(ns delilah.dei.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [clj-time.format :as f]
            [etaoin.api      :as api]
            [etaoin.keys     :as k]

            [delilah.common.parser :as cparser]
            [delilah.dei.parser    :as parser]))

(defn log-in [driver {:keys [user pass] :as ctx}]
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName})
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"})))

(defn do-task []
  (let [ctx (-> (io/resource "config.edn")
                slurp
                (edn/read-string))]
    (api/with-driver
      :firefox {:headless true
                :path-driver "resources/geckodriver"} driver
      (let [dom (-> (log-in driver ctx)
                    api/get-source
                    cparser/parse)]
        {:property-info (parser/property-info dom)
         :bills         (parser/bills dom)}))))





