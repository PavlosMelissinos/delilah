(ns delilah.gr.dei.bill
  (:require [clojure.spec.alpha :as s]

            [clj-http.client :as http]
            [java-time :as t]
            [taoensso.timbre :as log]
            [pdfboxing.common :as common]

            [delilah.gr.dei.cookies :as cookies]
            [delilah.gr.dei.pdf :as pdf]))

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

(defn enrich [{:keys [pdf-url bill-date] :as bill} {:keys [cookies ::pdf/areas] :as cfg}]
  (let [pdfdoc (fetch-file pdf-url cookies)
        version (pdf/version bill-date)]
    (log/info "Enriching bill from " bill-date ". Using parser " version)
    (merge bill
           {:pdf-contents pdfdoc}
           (pdf/parse-text pdfdoc {:version version
                                   :areas areas}))))
(s/fdef enrich
  :args (s/cat :bill (s/keys
                      :req-un [::pdf-url ::bill-date])
               :cfg  (s/keys
                      :req-un [::cookies ::pdf/areas])))

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

(defn date [bill]
  (if-let [dt (:bill-date bill)]
    (-> dt t/local-date-time (t/truncate-to :days))))
(s/fdef bill-date
  :args (s/cat :bill (s/or :dei-bill ::dei-bill
                           :mail-bill ::mail-bill))
  :ret  #(or (t/local-date-time? %) (nil? %)))

(defn contemporary-mail [dei-bill mail-bills]
  (let [start-of-bill-day (date dei-bill)]
    (log/info (str "Looking for matching mail bills around " start-of-bill-day))
    (first (filter #(t/after? (t/plus start-of-bill-day (t/weeks 1))
                              (date-received %)
                              start-of-bill-day)
                   mail-bills))))
