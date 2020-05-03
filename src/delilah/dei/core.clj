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
  (log/info "Firing up DEI sign-in page...")
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName}))
  (log/info "Signing into DEI account...")
  (doto driver
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"}))
  (log/info "Connected!")
  driver)

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

(defn collect-browser-data [{:keys [cache-dir] :as ctx}]
  (let [download-dir (str cache-dir "/downloads")]
    (log/info "Loading web driver...")
    (api/with-driver
      :firefox {:headless true
                :path-driver "resources/geckodriver"
                :load-strategy :none
                :download-dir download-dir} driver
      (let [dom  (->> (log-in driver ctx)
                      api/get-source
                      cparser/parse)
            _     (log/info "Throwing out the garbage...")
            data  (parser/parse dom)
            bills (map #(assoc % :download-dir download-dir) (:bills data))
            data  (assoc data :bills bills)]
        (log/info "Downloading bill files")
        (doseq [bill bills]
          (download-bill driver bill))
        data))))

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