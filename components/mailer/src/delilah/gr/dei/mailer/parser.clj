(ns delilah.gr.dei.mailer.parser
  (:require [clojure.string :as str]
            [hickory.select :as hs]
            [hickory-css-selectors :as hcs]
            [delilah.common.parser :as cparser]))

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

(defn dom [email]
  (-> email :body :body cparser/parse))

(defn parse [dom]
  (let [fragments (->> (hs/select (hcs/parse-css-selector ".wrapper td p") dom)
                       (map :content)
                       (filter valid-field?)
                       (map (juxt field-key field-value))
                       (into {}))]
    (reduce-kv (fn [m k v]
                 (assoc m k (get fragments v)))
               {}
               codes)))
