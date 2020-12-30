(ns delilah.gr.dei.mailer.parser
  (:require [clojure.string :as str]
            [hickory.select :as hs]
            [hickory-css-selectors :as hcs]
            [delilah.common.parser :as cparser]
            [java-time :as t]))

(def codes
  {:contract-account "Contract Account"
   :address          "Address"
   :e-payment-code   "e-payment code"
   :amount           "Amount"
   :expiration-date  "Expiration date"})

(defn valid-field? [field]
  (and #(> (count %) 1)
       #(= (-> % first :tag) :b)))

(defn field-key [fragment]
  (-> fragment first :content first))

(defn field-value [fragment]
  (->> fragment
       (map #(cond
               (string? %) (str/trim %)
               (map? %)    (-> % :content first)))
       (remove nil?)
       rest
       str/join))

(defn parse2019 [dom]
  (let [codes         [:contract-account :e-payment-code :amount :expiration-date]
        fragments-raw (hs/select (hcs/parse-css-selector ".EBill td b") dom)
        fragments     (->> (take 4 fragments-raw)
                           (map :content)
                           (map first)
                           (zipmap codes)
                           #_(filter valid-field?)
                           #_(map (juxt field-key field-value))
                           #_(into {}))]
    fragments))

(defn parse2020 [dom]
  (let [fragments-raw (hs/select (hcs/parse-css-selector ".wrapper td p") dom)
        fragments     (->> (map :content fragments-raw)
                           (filter valid-field?)
                           (map (juxt field-key field-value))
                           (into {}))]
    (reduce-kv (fn [m k v]
                 (assoc m k (get fragments v)))
               {}
               codes)))

(defn parse [{:keys [date-received] :as email}]
  (log/info (str "Parsing email of " date-received))
  (let [dom (-> email :body :body cparser/parse)]
    (cond
      (t/after? (t/instant date-received) (t/instant (t/zoned-date-time 2019 12 1)))
      (parse2020 dom)

      :else
      (parse2019 dom))))
