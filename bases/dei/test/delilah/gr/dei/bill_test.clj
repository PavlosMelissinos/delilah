(ns delilah.gr.dei.bill-test
  (:require [delilah.gr.dei.bill :as sut]
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
