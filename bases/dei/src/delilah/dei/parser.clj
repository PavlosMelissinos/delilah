(ns delilah.dei.parser
  (:require [clojure.string :as str]
            [java-time :as t]
            [hickory.select :as hs]
            [hickory-css-selectors :as hcs]
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
      str/trim))


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
  (let [fragment (distinct (map #(dissoc % :content) fragment))]
    (map bill fragment)))


;;;;;;;;;;;;;;; Selectors ;;;;;;;;;;;;;;;

(def css-selectors
  {::d/customer-codes       "#ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes option"
   ::d/active-customer-code "#ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes option"
   ::d/contract             "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value"
   ::d/customer-name        "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value"
   ::d/street               "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value"
   ::d/street-number        "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value"
   ::d/city                 "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value"
   ::d/bills                "#ctl00_ctl00_Site_Main_Main_CustomerCodeBills_CustomerCodeBillsContainer .BillsContainer .BillItem>a"})

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
                (fn [parse-fn selector] (parse-fn (hs/select (hcs/parse-css-selector selector) dom)))
                parsers
                css-selectors)]
    {:base-url      "https://www.dei.gr/EBill"
     :customer-code (::d/active-customer-code parsed)
     :property-info (select-keys parsed [::d/contract
                                         ::d/customer-name
                                         ::d/street
                                         ::d/street-number
                                         ::d/city])
     :bills         (::d/bills parsed)}))
