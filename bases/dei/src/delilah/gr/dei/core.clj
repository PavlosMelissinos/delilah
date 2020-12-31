(ns delilah.gr.dei.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]

            [delilah.common.parser :as cparser]
            [delilah.gr.dei.cookies :as cookies]
            [delilah.gr.dei :as ds]
            [delilah.gr.dei.parser :as parser]
            [delilah.gr.dei.mailer.api :as mailer-api]
            [delilah.gr.dei.mailer :as mailer]))


(defn dom [{:keys [cookies] :as ctx}]
  (let [endpoint  "https://ebill.dei.gr/Default.aspx"
        cookies   (or cookies (cookies/serve ctx))
        resp      (http/post endpoint
                             {:redirect-strategy :lax
                              :content-type      :json
                              :headers           {"Cookie" (cookies/->string cookies)}})]
    (-> resp :body cparser/parse)))
(s/fdef dom
  :args (s/cat :ctx (s/keys
                     :opt-un [::cookies])))

(defn- fetch-file
  "Makes an HTTP request and fetches a binary object."
  ([url]
   (fetch-file url nil))
  ([url cookies]
   (log/info (str "Fetching " url))
   (let [headers (when cookies {:headers {"Cookie" (cookies/->string cookies)}})
         options (merge {:as               :byte-array
                         :throw-exceptions false}
                        headers)
         resp    (http/get url options)]
     (when (= (:status resp) 200)
       (:body resp)))))

(defn- enrich-bill [{:keys [pdf-url] :as bill} {:keys [cookies]}]
  (assoc bill
         :pdf-contents (fetch-file pdf-url cookies)))
(s/fdef enrich-bill
  :args (s/cat :bill (s/keys
                      :req-un [::pdf-url])
               :cfg  (s/keys
                      :req-un [::cookies])))

(defn save-pdf!
  "Downloads and stores a pdf on disk."
  [{:keys [pdf-contents]} filepath]
  (let [filepath-partial (str filepath ".part")]
    (log/info (str "Saving pdf as " filepath))
    (io/delete-file filepath true)
    (io/delete-file filepath-partial true)
    (io/copy pdf-contents (io/file filepath))
    (log/info (str "PDF saved!"))
    filepath))

(defn latest-bill [{:keys [bills]}]
  (->> bills (sort-by :bill-date) reverse first))

(defn enrich! [{:keys [bills] :as data}
               {::mailer/keys [enrich?] :as cfg}]
  (let [bill-mails     (when enrich? (mailer-api/do-task cfg))
        enriched-bills (map #(enrich-bill % cfg) bills)]
    (assoc data
           :bills (map merge enriched-bills (concat bill-mails (repeat {}))))))
(s/fdef enrich!
  :args (s/cat :data (s/keys :req-un [::bills])
               :cfg  (s/keys :req-un [::cookies]
                             :opt    [::mailer/enrich?])))

(defn scrape [{:keys [cookies] :as ctx}]
  (let [data (try
               (-> ctx dom parser/parse)
               (catch Exception _
                 (log/info "Failed to parse page! Cookies might be stale...")
                 (cookies/with-session-bake ctx)
                 (log/info "Parsing page (second attempt)...")
                 (-> ctx dom parser/parse)))
        cfg  (assoc ctx :cookies (or cookies (cookies/serve ctx)))]
    (enrich! data cfg)))

(defn load-cfg [cfg]
  (-> (io/resource "config.edn")
      slurp
      (edn/read-string)
      (merge cfg)
      (update-in [:driver :path-driver] fs/expand-home)
      (update :delilah/cache-dir fs/expand-home)))

(defn extract [{:keys [save-files?] :as cfg}]
  (let [cfg           (load-cfg cfg)
        data          (scrape cfg)
        customer-code (:customer-code data)
        download-dir  (str/join "/"
                                [(:delilah/cache-dir cfg)
                                 "dei"
                                 "downloads"])]
    (when save-files?
      (doseq [{:keys [bill-date] :as bill} (:bills data)]
        (save-pdf! bill (format "%s/%s_%s.pdf" download-dir customer-code bill-date))))
    data))
(s/fdef extract
  :args (s/cat :cfg ::ds/cfg))

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

  (def res (scrape ctx)))
