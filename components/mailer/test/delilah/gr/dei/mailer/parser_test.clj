(ns delilah.gr.dei.mailer.parser-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]

            [delilah.gr.dei.mailer.parser :as sut]
            [delilah.common.parser :as cparser]))


(def dom (-> "bill-mail.html"
             io/resource
             slurp
             cparser/parse))

(def dom2 (-> "bill-mail-2019-10.html"
             io/resource
             slurp
             cparser/parse))

(deftest field-key
  (let [fragment [{:type :element, :attrs nil, :tag :b, :content ["Contract Account"]} {:type :element, :attrs nil, :tag :br, :content nil} "012345678901"]]
    (is (= :contract-account
           (sut/field-key fragment)))))

(deftest field-value
  (let [fragment [{:type :element, :attrs nil, :tag :b, :content ["Contract Account"]} {:type :element, :attrs nil, :tag :br, :content nil} "012345678901"]]
    (is (= "012345678901"
           (sut/field-value fragment)))))

(deftest test-parse2019
  (is (= {:contract-account "012345678901",
          :e-payment-code "RF00000000000012345678901",
          :amount "150,00 €",
          :expiration-date "11/11/2019"}
         (sut/parse2019 dom2))))

(deftest test-parse2020
  (is (= {:contract-account "012345678901",
          :address "ΣΤΟΥ ΔΙΑΟΛΟΥ ΤΗ ΜΑΝΑ, 54666, ΚΟΛΑΣΗ",
          :e-payment-code "RF00000000000012345678901",
          :amount "150,00 €",
          :expiration-date "19/01/2021"}
         (sut/parse2020 dom))))
