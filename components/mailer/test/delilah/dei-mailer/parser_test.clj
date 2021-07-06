(ns delilah.dei-mailer.parser-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]

            [delilah.dei-mailer.parser :as sut]
            [delilah.common-parser.interface :as cparser]
            [clojure.string :as str]))


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

(comment
  (let [coll-of-maps [{:k1 1 :k2 2 :k3 3 :k4 4}
                      {:k1 1 :k2 2}
                      {:k1 1 :k2 2 :k3 3}]
        a-map         {:k1 1
                       :k2 2
                       :k3 3}]
    (filter #(and (= (:k1 %) (:k1 a-map))
                  (= (:k2 %) (:k2 a-map))
                  (= (:k3 %) (:k3 a-map))) coll-of-maps))

  (let [coll-of-maps [{:k1 1 :k2 2 :k3 3 :k4 4}
                      {:k1 1 :k2 2}
                      {:k1 1 :k2 2 :k3 3}]
        a-map         {:k1 1
                       :k2 2
                       :k3 3}
        ks           [:k1 :k2 :k3]]
    (filter #(= (select-keys % ks) (select-keys a-map ks)) coll-of-maps))

  (let [coll-of-maps [{:k1 1 :k2 2 :k3 3 :k4 4}
                      {:k1 1 :k2 2}
                      {:k1 1 :k2 2 :k3 3}]
        a-map         {:k1 1
                       :k2 2
                       :k3 3}
        ks           [:k1 :k2 :k3]]
    {:naive-filter
     :select-keys
     :data-diff}
    (remove #(-> % (clojure.data/diff a-map) second) coll-of-maps))

  (let [coll-of-maps [{:k1 1 :k2 2 :k3 3 :k4 4}
                      {:k1 1 :k2 2}
                      {:k1 1 :k2 2 :k3 3}
                      {:k1 1 :k2 2 :k3 nil}]
        a-map         {:k1 1 :k2 2 :k3 3}
        ks           [:k1 :k2 :k3]]
    (= (filter #(and (= (:k1 %) (:k1 a-map))
                     (= (:k2 %) (:k2 a-map))
                     (= (:k3 %) (:k3 a-map))) coll-of-maps)
       (filter #(= (select-keys % ks) (select-keys a-map ks)) coll-of-maps)
       (remove #(-> % (clojure.data/diff a-map) second) coll-of-maps)))

  )


(comment
  (str "alal""lala""lala")
  (def glb-str "-XX:+UseG1GC")

  (->> (clojure.string/split (subs glb-str 1) #":")
       (drop 1)))
