(ns delilah.dei.parser
  (:require [clojure.string :as str]
            [java-time :as t]
            [hickory.select :as hs]
            [hickory-css-selectors :as hcs]))

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
  {:delilah/customer-codes       "#ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes option"
   :delilah/active-customer-code "#ctl00_ctl00_Site_Main_Main_UserCustomerCodesList1_lstUserCustomerCodes option"
   :delilah/contract             "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value"
   :delilah/customer-name        "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value"
   :delilah/street               "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value"
   :delilah/street-number        "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value"
   :delilah/city                 "#ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value"
   :delilah/bills                "#ctl00_ctl00_Site_Main_Main_CustomerCodeBills_CustomerCodeBillsContainer .BillsContainer .BillItem>a"})

(def parsers
  {:delilah/customer-codes       customer-info
   :delilah/active-customer-code active-customer-code
   :delilah/contract             property-info
   :delilah/customer-name        property-info
   :delilah/street               property-info
   :delilah/street-number        property-info
   :delilah/city                 property-info
   :delilah/bills                bills})


(defn parse [dom]
  (let [parsed (merge-with
                (fn [parse-fn selector] (parse-fn (hs/select (hcs/parse-css-selector selector) dom)))
                parsers
                css-selectors)]
    {:base-url      "https://www.dei.gr/EBill"
     :customer-code (:delilah/active-customer-code parsed)
     :property-info (select-keys parsed [:delilah/contract
                                         :delilah/customer-name
                                         :delilah/street
                                         :delilah/street-number
                                         :delilah/city])
     :bills         (:delilah/bills parsed)}))
