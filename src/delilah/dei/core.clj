(ns delilah.dei.core
  (:require [clojure.tools.logging :as log]

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

(defn download-file [url dest]
  (let [file-out (clojure.java.io/file dest)]
    (-> url
        (http/get {:as :stream})
        :body
        (clojure.java.io/copy file-out))
    (api/wait-predicate #(.exists file-out))))

(defn do-task [ctx]
  (api/with-driver
    :firefox {:headless true
              :path-driver "resources/geckodriver"
              :download-dir "/tmp"} driver
    (let [dom  (-> (log-in driver ctx)
                   api/get-source
                   cparser/parse)
          data {:base-url      "https://www.dei.gr/EBill"
                :property-info (parser/property-info dom)
                :bills         (parser/bills dom)}]
      (doseq [{:keys [pdf-url bill-date] :as bill} (:bills data)]
        (let [filepath (->> (clojure.string/split pdf-url #"FileName=")
                            last
                            (format "/tmp/%s"))]
          (log/info (format "Downloading %s to %s" pdf-url filepath))
          (download-file pdf-url filepath)))
      data)))

(comment
  (def ctx (-> (io/resource "config.edn")
               slurp
               (edn/read-string)))
  (def driver (api/firefox {:headless true
                            :path-driver "resources/geckodriver"})))