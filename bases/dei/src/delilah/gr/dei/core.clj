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
            [delilah.gr.dei.mailer :as mailer]
            [java-time :as t]))


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

;; Bills

(s/def ::date-received t/instant?)
(s/def ::mail-bill (s/keys :req-un [::date-received]))
(s/def ::bill-date t/local-date?)
(s/def ::dei-bill (s/keys :req-un [::bill-date]))

(defn date-received [bill]
  (t/local-date-time (:date-received bill) (t/zone-id)))
(s/fdef bill-date
  :args (s/cat :bill (s/or :dei-bill ::dei-bill
                           :mail-bill ::mail-bill))
  :ret  t/instant?)

(defn bill-date [bill]
  (if-let [dt (:bill-date bill)]
    (-> dt t/local-date-time (t/truncate-to :days))))
(s/fdef bill-date
  :args (s/cat :bill (s/or :dei-bill ::dei-bill
                           :mail-bill ::mail-bill))
  :ret  #(or (t/local-date-time? %) (nil? %)))

(s/def ::bills (s/coll-of ::dei-bill))

(defn contemporary-mail [dei-bill mail-bills]
  (let [start-of-bill-day (bill-date dei-bill)]
    (log/info (str "Looking for matching mail bills around " start-of-bill-day))
    (first (filter #(t/after? (t/plus start-of-bill-day (t/weeks 1))
                              (date-received %)
                              start-of-bill-day)
                   mail-bills))))

(defn join-bill-data [mail-bills dei-bills]
  (let [mail-bills      (sort-by :date-received mail-bills)
        dei-bills       (sort-by :bill-date dei-bills)
        full-bills      (map #(merge % (contemporary-mail % mail-bills)) dei-bills)
        legacy-date     (-> dei-bills first bill-date) ;; date of oldest bill on dei.gr
        legacy-bills    (filter #(t/before? (date-received %) legacy-date) mail-bills)
        all-bills       (concat full-bills legacy-bills)]
    (->> (sort-by #(or (bill-date %) (date-received %)) all-bills)
         reverse)))
(s/fdef join-bill-data
  :args (s/cat :mail-bills (s/coll-of (s/keys :req-un [::date-received]))
               :dei-bills  (s/coll-of (s/keys :req-un [::bill-date]))))

(defn enrich-bills! [bills {::mailer/keys [enrich?] :as cfg}]
  (let [mail-bills (when enrich? (mailer-api/do-task cfg))
        dei-bills  (map #(enrich-bill % cfg) bills)]
    (join-bill-data mail-bills dei-bills)))
(s/fdef enrich-bills!
  :args (s/cat :bills ::bills
               :cfg   (s/keys :req-un [::cookies]
                              :opt    [::mailer/enrich?])))

(defn latest-bill [{:keys [bills]}]
  (->> bills (sort-by :bill-date) reverse first))
(s/fdef latest-bill
  :args (s/cat :dei-map (s/keys :req-un [::bills])))

;;; General

(defn scrape [{:keys [cookies] :as ctx}]
  (let [data (try
               (-> ctx dom parser/parse)
               (catch Exception _
                 (log/info "Failed to parse page! Cookies might be stale...")
                 (cookies/with-session-bake ctx)
                 (log/info "Parsing page (second attempt)...")
                 (-> ctx dom parser/parse)))
        cfg  (assoc ctx :cookies (or cookies (cookies/serve ctx)))]
    (update data :bills enrich-bills! cfg)))

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
