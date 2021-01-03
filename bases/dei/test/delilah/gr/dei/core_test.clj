(ns delilah.gr.dei.core-test
  (:require [delilah.gr.dei.core :as sut]
            [clojure.test :refer :all]
            [java-time :as t]))

(deftest test-contemporary-mail
  (is (= {:date-received (t/instant #inst "2020-12-23T12:18:38")}
         (sut/contemporary-mail {:bill-date (t/local-date "2020-12-22")}
                                [{:date-received (t/instant #inst "2020-10-19T19:19:18")}
                                 {:date-received (t/instant #inst "2020-12-23T12:18:38")}
                                 {:date-received (t/instant #inst "2020-12-24T12:18:27")}])))

  (is (= nil
         (sut/contemporary-mail {:bill-date (t/local-date "2020-08-20")}
                                [{:date-received (t/instant #inst "2020-10-19T19:19:18")}
                                 {:date-received (t/instant #inst "2020-12-23T12:18:38")}
                                 {:date-received (t/instant #inst "2020-12-24T12:18:27")}]))))

(deftest test-join-bill-data
  (let [dei-bills  [{:bill-date (t/local-date "2020-12-22")}
                    {:bill-date (t/local-date "2020-10-16")}
                    {:bill-date (t/local-date "2020-08-20")}
                    {:bill-date (t/local-date "2020-06-18")}
                    {:bill-date (t/local-date "2020-04-22")}
                    {:bill-date (t/local-date "2020-02-17")}]
        mail-bills [{:date-received (t/instant #inst "2019-03-02T09:31:12")}
                    {:date-received (t/instant #inst "2019-04-18T12:15:29")}
                    {:date-received (t/instant #inst "2019-06-18T14:31:53")}
                    {:date-received (t/instant #inst "2019-08-21T12:34:49")}
                    {:date-received (t/instant #inst "2019-10-17T13:40:03")}
                    {:date-received (t/instant #inst "2019-12-19T13:02:34")}
                    {:date-received (t/instant #inst "2020-02-18T14:22:43")}
                    {:date-received (t/instant #inst "2020-04-23T12:05:42")}
                    {:date-received (t/instant #inst "2020-06-19T11:06:27")}
                    {:date-received (t/instant #inst "2020-10-19T13:54:53")}
                    {:date-received (t/instant #inst "2020-10-19T19:19:18")}
                    {:date-received (t/instant #inst "2020-12-23T12:18:38")}]
        expected   [{:date-received (t/instant #inst "2020-12-23T12:18:38")
                     :bill-date (t/local-date "2020-12-22")}
                    {:date-received (t/instant #inst "2020-10-19T13:54:53")
                     :bill-date (t/local-date "2020-10-16")}
                    {:bill-date (t/local-date "2020-08-20")}
                    {:date-received (t/instant #inst "2020-06-19T11:06:27")
                     :bill-date (t/local-date "2020-06-18")}
                    {:date-received (t/instant #inst "2020-04-23T12:05:42")
                     :bill-date (t/local-date "2020-04-22")}
                    {:date-received (t/instant #inst "2020-02-18T14:22:43")
                     :bill-date (t/local-date "2020-02-17")}
                    {:date-received (t/instant #inst "2019-12-19T13:02:34")}
                    {:date-received (t/instant #inst "2019-10-17T13:40:03")}
                    {:date-received (t/instant #inst "2019-08-21T12:34:49")}
                    {:date-received (t/instant #inst "2019-06-18T14:31:53")}
                    {:date-received (t/instant #inst "2019-04-18T12:15:29")}
                    {:date-received (t/instant #inst "2019-03-02T09:31:12")}]]
        (is (= expected
               (sut/join-bill-data mail-bills dei-bills)))))
