(ns delilah.common-parser.interface
  (:require [clojure.spec.alpha :as s]
            [hickory.core :as html]))

(defn parse [doc]
  (when doc
    (-> doc html/parse html/as-hickory)))
(s/fdef parse
  :args (s/cat :doc string?))
