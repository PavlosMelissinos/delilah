(ns delilah.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [clj-time.format :as f]
            [etaoin.api      :as api]
            [etaoin.keys     :as k]))

(defn log-in [driver {:keys [user pass] :as ctx}]
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName})
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"})))

(defn latest-bill-date [driver ctx]
  (let [formatter (f/formatter "dd.MM.yyyy")]
    (-> (api/query driver [{:tag :div :fn/has-class "BillItem"}
                           {:tag :a}])
        (#(api/get-element-attr-el driver % :title))
        (clojure.string/split #" ")
        second
        (#(f/parse formatter %)))))

(defn do-task []
  #_(let [driver (api/firefox {:path-driver "resources/geckodriver"})])
  (api/with-firefox-headless
    {:path-driver "resources/geckodriver"}
    driver
    (let [ctx (-> (io/resource "config.edn")
                  slurp
                  (edn/read-string))]
      (-> driver
          (log-in ctx)
          (latest-bill-date ctx)))))
