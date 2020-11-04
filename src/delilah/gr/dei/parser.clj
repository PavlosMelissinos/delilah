(ns delilah.dei.parser
  (:require [java-time :as t]
            [hickory.select :as hs]))

(defn format-date [date]
  (t/format "YYYY-MM-dd" date))

;;;;;;;;;;;;;;; Selectors ;;;;;;;;;;;;;;;

(def selectors
  {:customer-codes (hs/descendant
                     (hs/id "ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes")
                     (hs/tag :option))
   :property-info {:title                  (hs/descendant
                                             (hs/class "header_row")
                                             (hs/class "header"))
                   :contract-account       (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value")
                   :customer-name          (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value")
                   :customer-street        (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value")
                   :customer-street-number (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value")
                   :customer-city          (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value")}
   :bills         (hs/descendant
                    (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeBills_CustomerCodeBillsContainer")
                    (hs/class "BillsContainer")
                    (hs/class "BillItem")
                    (hs/find-in-text #"Έκδοση"))})

(defn customer-info [dom]
  (->> dom
       (hs/select (:customer-codes selectors))
       (map #(hash-map :customer-code ((comp :value :attrs) %)
                       :selected ((comp :selected :attrs) %)
                       :description ((comp first :content) %)))))

(defn customer-codes [dom]
  (->> dom
       customer-info
       (map :customer-code)))

(defn active-customer-code [dom]
  (->> dom
       customer-info
       (filter #(= (:selected %) "selected"))
       first
       :customer-code))

(defn property-info-parse [fragment selector]
  (-> (hs/select selector fragment)
      first
      :content
      first
      clojure.string/trim))

(defn property-info [dom]
  (->> selectors
       :property-info
       (reduce-kv
         (fn [m k v]
           (assoc m k (property-info-parse dom v)))
         {})))

(defn bill-date [bill]
  (let [formatter "dd.MM.yyyy"]
    (-> (get-in bill [:attrs :title])
        (clojure.string/split #" ")
        second
        ((partial t/local-date formatter)))))

(defn pdf-url [bill]
  (str "https://www.dei.gr/EBill/" (get-in bill [:attrs :href])))

(defn bill [fragment]
  {:bill-date (bill-date fragment)
   :pdf-url   (pdf-url fragment)})

(defn bills [dom]
  (->> dom
       (hs/select (:bills selectors))
       (map bill)))

(defn dest-file [bill customer-code]
  (let [bill-date (->> bill :bill-date format-date)]
    (format "%s_%s.pdf" customer-code bill-date)))

(defn parse [dom]
  (let [customer-code (active-customer-code dom)]
    {:base-url      "https://www.dei.gr/EBill"
     :customer-code customer-code
     :property-info (property-info dom)
     :bills         (map #(assoc % :dest-file (dest-file % customer-code)) (bills dom))}))
