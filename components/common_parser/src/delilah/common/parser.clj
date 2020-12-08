(ns delilah.common.parser
  (:require [hickory.core :as html]))

(defn parse [doc]
  (-> doc html/parse html/as-hickory))
