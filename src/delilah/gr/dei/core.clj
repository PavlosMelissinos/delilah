(ns delilah.gr.dei.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]

            [etaoin.api :as api]
            [etaoin.keys :as k]
            [java-time :as t]

            [delilah.common.parser :as cparser]
            [delilah.gr.dei.parser :as parser]))

(defn log-in [driver {:keys [user pass] :as ctx}]
  (log/info "Firing up DEI sign-in page...")
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName}))
  (log/info (format "Signing into DEI account as %s..." user))
  (doto driver
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"}))
  (log/info "Connected!")
  driver)

(defn ->dom [driver ctx]
  (->> (log-in driver ctx)
       api/get-source
       cparser/parse))

(defn download-bill [driver {:keys [pdf-url download-dir dest-file] :as bill}]
  (let [filepath         (format "%s/%s" download-dir dest-file)
        filepath-partial (str filepath ".part")]
    (io/delete-file filepath true)
    (io/delete-file filepath-partial true)
    (log/info (format "Downloading %s to %s" pdf-url filepath))
    (io/make-parents filepath)
    (api/go driver pdf-url)
    (api/wait-predicate #(and (.exists (io/file filepath))
                              (not (.exists (io/file filepath-partial)))))
    filepath))

#_(defn download-bill-for-date [driver dom date]
  (->> dom
       parser/bills
       (filter #(= (:bill-date %) (t/local-date date)))))

(defn collect-browser-data [{:keys [download-dir] :as ctx}]
  (log/info "Loading web driver...")
  (api/with-driver
    :firefox {:headless true
              :path-driver "resources/webdrivers/geckodriver" ;TODO: not working with io/resource, fix
              :load-strategy :none
              :download-dir download-dir} driver
    (let [data  (parser/parse (->dom driver ctx))
          bills (map #(assoc % :download-dir download-dir) (:bills data))
          data  (assoc data :bills bills)]
      (log/info "Downloading bill files")
      (doseq [bill bills]
        (download-bill driver bill)))))

(defn do-task [ctx]
  (let [browser-data (collect-browser-data ctx)]
    browser-data
    ;TODO: parse pdfs
  ))

(comment
  (def ctx (let [ctx-base (-> (io/resource "config.edn")
                              slurp
                              (edn/read-string))
                 secrets  (-> (io/resource "secrets.edn")
                              slurp
                              (edn/read-string)
                              :dei)]
             (merge ctx-base secrets)))
  (def driver (api/firefox {:headless false
                            :path-driver "resources/webdrivers/geckodriver"}))
  (def dom (->dom driver ctx))
  (def data (do-task ctx)))
