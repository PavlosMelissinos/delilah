(ns delilah.dei-mailer.parser
  (:require [clojure.string :as str]

            [hickory.select :as hs]
            [hickory-css-selectors :as hcs]
            [java-time :as t]
            [taoensso.timbre :as log]

            [delilah.common-parser.interface :as cparser]))

(java-time/when-joda-time-loaded
  (extend-type org.joda.time.ReadableInstant
    Inst (inst-ms* [inst] (.getMillis inst))))

(defn valid-field? [field]
  (and #(> (count %) 1)
       #(= (-> % first :tag) :b)))

(defn field-key [fragment]
  (if-let [key-raw (-> fragment first :content first)]
    (-> key-raw str/lower-case (#(str/replace % #"[ ]+" "-")) keyword)))

(defn field-value [fragment]
  (->> fragment
       (map #(cond
               (string? %) (str/trim %)
               (map? %)    (-> % :content first)))
       (remove nil?)
       rest
       str/join))

(defn parse2019 [dom]
  (log/info "Legacy mode on! (before 2019-12)")
  (let [codes         [:contract-account :e-payment-code :amount :expiration-date]
        fragments-raw (hs/select (hcs/parse-css-selector ".EBill td b") dom)
        fragments     (->> (map :content fragments-raw)
                           (map first)
                           (zipmap codes))]
    fragments))

(defn parse2020 [dom]
  (let [codes         [:contract-account :e-payment-code :amount :expiration-date :address]
        fragments-raw (hs/select (hcs/parse-css-selector ".wrapper td p") dom)
        fragments     (->> (map :content fragments-raw)
                           (filter valid-field?)
                           (map (juxt field-key field-value))
                           (into {}))]
    (select-keys fragments codes)))

(defn parse [{:keys [date-received subject] :as email}]
  (log/info (str "Parsing email of " date-received
                 ", with subject " subject))
  (let [date-received (t/instant date-received)
        dom           (some-> email :body :body cparser/parse)
        bill          (cond
                        (t/after? date-received
                                  (t/instant (t/zoned-date-time 2019 12 1)))
                        (parse2020 dom)

                        :else
                        (parse2019 dom))]
    (assoc bill :date-received date-received)))
