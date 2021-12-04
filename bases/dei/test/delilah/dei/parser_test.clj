(ns delilah.dei.parser-test
  (:require [clojure.test :refer [deftest is]]

            [java-time :as t]

            [delilah.dei.parser :as sut]))

(deftest test-format-date
  (let [datestr "2020-02-17"]
    (is (= (-> (t/local-date datestr) sut/format-date)
           datestr))))

(deftest test-customer-info
  (let [fragment [{:type :element, :attrs {:selected "selected", :value "0123456789"}, :tag :option, :content ["246813579 - Φούφουτος Ιωάννης - Στου διαόλου τη μάνα 666"]}]]
    (is (= [{:description   "246813579 - Φούφουτος Ιωάννης - Στου διαόλου τη μάνα 666",
             :selected      "selected",
             :customer-code "0123456789"}]
           (sut/customer-info fragment)))))

(deftest test-active-customer-code
  (let [fragment [{:type :element, :attrs {:selected "selected", :value "0123456789"}, :tag :option, :content ["246813579 - Φούφουτος Ιωάννης - Στου διαόλου τη μάνα 666"]}]]
    (is (= "0123456789"
           (sut/active-customer-code fragment)))))

(deftest test-property-info
  (let [fragment [{:type :element, :attrs {:id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerName_value"}, :tag :span, :content ["ΦΟΥΦΟΥΤΟΣ ΙΩΑΝΝΗΣ"]}]]
    (is (= "ΦΟΥΦΟΥΤΟΣ ΙΩΑΝΝΗΣ"
           (sut/property-info fragment))))
  (let [fragment [{:type :element, :attrs {:id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreet_value"}, :tag :span, :content ["ΣΤΟΥ ΔΙΑΟΛΟΥ ΤΗ ΜΑΝΑ"]}]]
    (is (= "ΣΤΟΥ ΔΙΑΟΛΟΥ ΤΗ ΜΑΝΑ"
           (sut/property-info fragment))))
  (let [fragment [{:type :element, :attrs {:id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerStreetNumber_value"}, :tag :span, :content ["666"]}]]
    (is (= "666"
           (sut/property-info fragment))))
  (let [fragment [{:type :element, :attrs {:id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCity_value"}, :tag :span, :content ["ΠΟΛΙΣ"]}]]
    (is (= "ΠΟΛΙΣ"
           (sut/property-info fragment))))
  (let [fragment [{:type :element, :attrs {:id "ctl00_ctl00_Site_Main_Main_CustomerCodeDetails_fvCustomerCodeDetails_CustomerCode_value"}, :tag :span, :content ["246813579"]}]]
    (is (= "246813579"
           (sut/property-info fragment)))))

(deftest test-bill-date
  (let [fragment {:type :element
                  :attrs {:target "_blank"
                          :href "UserBill.aspx?FileName=000000000000_20200217_000000000_1.pdf"
                          :title "Έκδοση 17.02.2020"}
                  :tag :a}]
    (is (= (t/local-date "2020-02-17")
           (sut/bill-date fragment)))))

(deftest test-bill
  (let [fragment {:type :element, :attrs {:target "_blank", :href "UserBill.aspx?FileName=name-of-file.pdf", :title "Έκδοση 18.12.2019"}, :tag :a, :content ["\r\n                                    Έκδοση 18.12.2019\r\n                                "]}]
    (is (= {:bill-date "2019-12-18",
            :pdf-url
            "https://www.dei.gr/EBill/UserBill.aspx?FileName=name-of-file.pdf"}
           (-> (sut/bill fragment)
               (update :bill-date #(t/format %)))))))
