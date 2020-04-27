(ns delilah.dei.parser
  (:require [java-time :as t]
            [hickory.select :as hs]))

(defn format-date [date]
  (t/format "YYYY-MM-dd" date))

;;;;;;;;;;;;;;; Selectors ;;;;;;;;;;;;;;;

(def selectors
  {:property-info {:title (hs/descendant
                            (hs/class "header_row")
                            (hs/class "header"))
                   :contract-account (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value")
                   :customer-name (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value")
                   :customer-street (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value")
                   :customer-street-number (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value")
                   :customer-city (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value")}
   :bills          (hs/descendant
                     (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeBills_CustomerCodeBillsContainer")
                     (hs/class "BillsContainer")
                     (hs/class "BillItem")
                     (hs/find-in-text #"Έκδοση"))})

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

(defn bills [dom]
  (let [formatter "dd.MM.yyyy"]
    (->> dom
         (hs/select (:bills selectors))
         (map #(hash-map :bill-date (-> (get-in % [:attrs :title])
                                        (clojure.string/split #" ")
                                        second
                                        ((partial t/local-date formatter)))
                         :pdf-url   (str "https://www.dei.gr/EBill/" (get-in % [:attrs :href])))))))
