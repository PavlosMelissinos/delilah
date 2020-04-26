(ns delilah.common.parser
  (:require [hickory.core :as html]))

(defn parse [doc]
  (-> (html/parse doc)
      html/as-hickory))
