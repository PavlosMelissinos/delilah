(ns delilah.dei.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]
            [java-time :as t]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]

            [delilah.common-parser.interface :as cparser]
            [delilah.dei.bill :as bill]
            [delilah.dei.cookies :as cookies]
            [delilah.dei-mailer.interface :as mailer]
            [delilah.dei.parser :as parser]
            [delilah.dei.pdf :as pdf]
            [delilah.dei.spec]))


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

;;; Bill data (pdf and mail)

(s/def :delilah.dei/bills (s/coll-of :delilah.dei/bill))

(defn join-bill-data [mail-bills dei-bills]
  (let [mail-bills      (sort-by :date-received mail-bills)
        dei-bills       (sort-by :bill-date dei-bills)
        full-bills      (map #(merge % (bill/contemporary-mail % mail-bills)) dei-bills)
        legacy-date     (-> dei-bills first bill/date) ;; date of oldest bill on dei.gr
        legacy-bills    (filter #(t/before? (bill/date-received %) legacy-date) mail-bills)
        all-bills       (concat full-bills legacy-bills)]
    (->> (sort-by #(or (bill/date %) (bill/date-received %)) all-bills)
         reverse)))
(s/fdef join-bill-data
  :args (s/cat :mail-bills (s/coll-of (s/keys :req-un [:delilah.dei/date-received]))
               :dei-bills  (s/coll-of (s/keys :req-un [:delilah.dei/bill-date]))))

(defn enrich-bills! [bills {::mailer/keys [enrich?] :as cfg}]
  (let [mail-bills (when enrich? (mailer-api/do-task cfg))
        dei-bills  (map #(bill/enrich % cfg) bills)]
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

(defn load-cfg [cfg]
  (let [base-fn   (fn [c] (-> (io/resource c) slurp edn/read-string))
        base-cfg  (base-fn "config.edn")
        pdf-areas (base-fn "coordinates.edn")]
    (-> (merge base-cfg {::pdf/areas pdf-areas} cfg)
        (update-in [:driver :path-driver] fs/expand-home)
        (update :delilah/cache-dir fs/expand-home))))

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

(defn extract [cfg]
  (let [{:keys [save-files? :delilah/cache-dir]
         :as cfg}     (load-cfg cfg)

        data          (scrape cfg)
        customer-code (:customer-code data)
        download-dir  (str/join "/" [cache-dir "dei" "downloads"])]
    (when save-files?
      (doseq [{:keys [bill-date] :as bill} (:bills data)]
        (save-pdf! bill (format "%s/%s_%s.pdf" download-dir customer-code bill-date))))
    data))
(s/fdef extract
  :args (s/cat :cfg :delilah.dei/cfg))

(comment
  (def ctx (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               edn/read-string
               load-cfg))

  (def res (scrape ctx)))
