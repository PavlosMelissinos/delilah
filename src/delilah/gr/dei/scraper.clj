(ns delilah.gr.dei.scraper
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]
            [etaoin.api :as api]
            [etaoin.keys :as k]
            [java-time :as t]
            [me.raynes.fs :as fs]

            [delilah.common.parser :as cparser]
            [delilah.gr.dei.parser :as parser]
            [hickory.core :as html]))


;;;;; START COOKIE STUFF ;;;;;

(defn log-in [driver {:delilah.gr.dei/keys [user pass] :as ctx}]
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

(defn cookie-file [{:delilah/keys [cache-dir provider]
                    :delilah.gr.dei/keys [user]
                    :as ctx}]
  (let [cache-dir (fs/expand-home cache-dir)]
    (clojure.string/join "/" [cache-dir "dei" "cookies" user])))

(defn refresh-cookies [driver {:delilah/keys [cache-dir]
                               :delilah.gr.dei/keys [user]
                               :keys [force]
                               :as ctx}]
  (log/info "Getting fresh cookies from the oven...")
  (let [cookies (-> (log-in driver ctx) api/get-cookies)]
    (-> ctx cookie-file fs/parent fs/mkdirs)
    (-> ctx cookie-file (spit cookies))
    cookies))

(defn with-refresh-cookies [{:keys [driver] :as ctx}]
  (api/with-driver (:type driver) (dissoc driver :type) d
    (refresh-cookies d ctx)))

(defn load-cookies [ctx]
  (log/info "Loading cached cookies...")
  (try
    (-> ctx cookie-file slurp edn/read-string)
    (catch Exception e
      (do
        (log/info (str "Cookies not found at " (cookie-file ctx)))
        (with-refresh-cookies ctx)))))

(defn- format-cookie [{:keys [name value] :as cookie}]
  (clojure.string/join "=" [name value]))

(defn format-cookies [cookies]
  (->> (map format-cookie cookies)
       (clojure.string/join "; " )))

;;;;; END COOKIE STUFF ;;;;;

(defn download-dir [{:delilah/keys [cache-dir] :as ctx}]
  (let [cache-dir (fs/expand-home cache-dir)]
    (clojure.string/join "/" [cache-dir "dei" "downloads"])))

(defn dom [{:keys [cookies] :as ctx}]
  (let [endpoint  "https://ebill.dei.gr/Default.aspx"
                                        ;endpoint "https://ebill.dei.gr/Login.aspx"
        cookies   (or cookies (load-cookies ctx))
        resp      (http/post endpoint
                             {:redirect-strategy :lax
                              :content-type      :json
                              :headers           {"Cookie" (format-cookies cookies)}})]
    (-> resp :body cparser/parse)))

(defn- fetch-file
  "Makes an HTTP request and fetches a binary object."
  ([url]
   (fetch-file url nil))
  ([url cookies]
   (log/info (str "Fetching " url))
   (let [headers (when cookies {:headers {"Cookie" (format-cookies cookies)}})
         options (merge {:as               :byte-array
                         :throw-exceptions false}
                        headers)
         resp    (http/get url options)]
     (if (= (:status resp) 200)
       (:body resp)))))

(defn- enrich-bill [{:keys [pdf-url] :as bill} {:keys [cookies] :as cfg}]
  (assoc bill
         :download-dir (download-dir cfg)
         :pdf-contents (fetch-file pdf-url cookies)))

(defn save-pdf!
    "Downloads and stores a pdf on disk."
  [{:keys [pdf-contents download-dir dest-file] :as file}]
  (let [filepath         (format "%s/%s" download-dir dest-file)
        filepath-partial (str filepath ".part")]
    (io/delete-file filepath true)
    (io/delete-file filepath-partial true)
    (io/copy pdf-contents (io/file download-dir dest-file))
    filepath))

(defn scrape [{:keys [cache-dir cookies] :as ctx}]
  (let [data (try
               (-> ctx dom parser/parse)
               (catch Exception e
                 (do
                   (log/info "Error parsing page! Trying to get fresh cookies...")
                   (with-refresh-cookies ctx)
                   (log/info "Retrying to parse page...")
                   (-> ctx dom parser/parse))))
        cfg  {:cache-dir cache-dir
              :cookies   (or cookies (load-cookies ctx))}]
    (update data :bills (fn [bills]
                          (map #(enrich-bill % cfg) bills)))))

(comment
  (def ctx (let [ctx-base (-> (io/resource "config.edn")
                              slurp
                              (edn/read-string)
                              (update-in [:driver :path-driver] fs/expand-home)
                              (update :delilah/cache-dir fs/expand-home))
                 secrets  (-> "~/.config/delilah/secrets.edn"
                              fs/expand-home
                              slurp
                              (edn/read-string))]
             (merge ctx-base secrets)))

  (def driver-spec {:headless true
                    :path-driver "resources/webdrivers/geckodriver"})

  (api/with-driver :firefox driver-spec d
    (refresh-cookies d ctx))

  (scrape ctx))
