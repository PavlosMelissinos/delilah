(ns delilah.gr.dei.parser
  (:require [java-time :as t]
            [hickory.select :as hs]
            [delilah :as d]))

(defn format-date [date]
  (t/format "YYYY-MM-dd" date))

;;; customer

(defn customer-info [fragment]
  (map #(hash-map :customer-code ((comp :value :attrs) %)
                  :selected ((comp :selected :attrs) %)
                  :description ((comp first :content) %))
       fragment))

(defn active-customer-code [fragment]
  (->> fragment
       customer-info
       (filter #(= (:selected %) "selected"))
       first
       :customer-code))

;;; property

(defn property-info [fragment]
  (-> fragment
      first
      :content
      first
      clojure.string/trim))


;;; bills

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

(defn bills [fragment]
  (map bill fragment))


;;;;;;;;;;;;;;; Selectors ;;;;;;;;;;;;;;;

(def selectors
  {::d/customer-codes       (hs/descendant
                             (hs/id "ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes")
                             (hs/tag :option))
   ::d/active-customer-code (hs/descendant
                             (hs/id "ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes")
                             (hs/tag :option))
   ::d/contract             (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value")
   ::d/customer-name        (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value")
   ::d/street               (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value")
   ::d/street-number        (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value")
   ::d/city                 (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value")
   ::d/bills                (hs/descendant
                             (hs/id "ctl00_ctl00_Site_Main_Main_CustomerCodeBills_CustomerCodeBillsContainer")
                             (hs/class "BillsContainer")
                             (hs/class "BillItem")
                             (hs/find-in-text #"Έκδοση"))})

(def parsers
  {::d/customer-codes       customer-info
   ::d/active-customer-code active-customer-code
   ::d/contract             property-info
   ::d/customer-name        property-info
   ::d/street               property-info
   ::d/street-number        property-info
   ::d/city                 property-info
   ::d/bills                bills})


(defn parse [dom]
  (let [parsed (merge-with
                (fn [parse-fn selector] (parse-fn (hs/select selector dom)))
                parsers
                selectors)]
    {:base-url      "https://www.dei.gr/EBill"
     :customer-code (::d/active-customer-code parsed)
     :property-info (select-keys parsed [::d/contract
                                         ::d/customer-name
                                         ::d/street
                                         ::d/street-number
                                         ::d/city])
     :bills         (::d/bills parsed)}))
