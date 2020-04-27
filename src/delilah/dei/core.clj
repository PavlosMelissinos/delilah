(ns delilah.dei.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]

            [clj-http.client :as http]
            [etaoin.api :as api]
            [etaoin.keys :as k]
            [java-time :as t]

            [delilah.common.parser :as cparser]
            [delilah.dei.parser :as parser]))

(defn log-in [driver {:keys [user pass] :as ctx}]
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName})
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"})))

(defn download-bill [driver {:keys [pdf-url download-dir dest-file] :as bill}]
  (let [filepath         (format "%s/%s" download-dir dest-file)
        filepath-partial (str filepath ".part")]
    (io/delete-file filepath "blurp")
    (io/delete-file filepath-partial "blurp")
    (log/info (format "Downloading %s to %s" pdf-url filepath))
    (io/make-parents filepath)
    (api/go driver pdf-url)
    (api/wait-predicate #(and (.exists (io/file filepath))
                              (not (.exists (io/file filepath-partial)))))
    filepath))

(defn collect-browser-data [{:keys [cache-dir] :as ctx}]
  (let [download-dir (str cache-dir "/downloads")]
    (api/with-driver
      :firefox {:headless true
                :path-driver "resources/geckodriver"
                :load-strategy :none
                :download-dir download-dir} driver
      (let [dom  (-> (log-in driver ctx)
                     api/get-source
                     cparser/parse)
            data {:base-url      "https://www.dei.gr/EBill"
                  :download-dir  download-dir
                  :customer-code (-> driver
                                     api/get-url
                                     (clj-http.links/read-link-params)
                                     :CustomerCode)
                  :property-info (parser/property-info dom)}
            bills (for [bill (parser/bills dom)]
                    (assoc bill
                      :download-dir download-dir
                      :dest-file (->> bill
                                      :bill-date
                                      parser/format-date
                                      (format "%s_%s.pdf" (-> data :customer-code)))))]
        (doseq [bill bills] (download-bill driver bill))
        (assoc data :bills bills)))))

(defn do-task [ctx]
  (let [browser-data (collect-browser-data ctx)]
    ;TODO: parse pdfs
    browser-data))

(comment
  (def ctx (-> (io/resource "config.edn")
               slurp
               (edn/read-string)))
  (def dom (-> (log-in driver ctx)
               api/get-source
               cparser/parse))
  (def data (do-task ctx))
  (def driver (api/firefox {:headless true
                            :path-driver "resources/geckodriver"})))