(ns delilah.dei.pdf
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]

            [java-time :as t]
            [pdfboxing.common :as common]
            [taoensso.timbre :as log])
  (:import (org.apache.pdfbox.text PDFTextStripper
                                   PDFTextStripperByArea)
           (java.awt Rectangle)))

(defn- area-text [doc {:keys [id x y w h page-number] :as area}]
  (let [page-number  (or page-number 0)
        rectangle    (Rectangle. x y w h)
        pdpage       (.getPage doc page-number)
        textstripper (doto (PDFTextStripperByArea.)
                       (.addRegion "region" rectangle)
                       (.extractRegions pdpage))]
    (.getTextForRegion textstripper "region")))

(defn- extract-by-areas
  "get text from a specified area of a PDF document"
  [pdfdoc areas]
  (with-open [doc (common/obtain-document pdfdoc)]
    (doall (map #(area-text doc %) areas))))

(defn version [date]
  (cond
    (t/after? date (t/local-date "2020-12-01")) "v2"
    (t/after? date (t/local-date "2019-12-01")) "v1"))
(s/fdef version
  :args (s/cat :date t/local-date?))

(defn- pos-int-or-zero? [n]
  (or (pos-int? n) (zero? n)))

(s/def ::id keyword?)
(s/def ::coord pos-int-or-zero?)
(s/def ::x ::coord)
(s/def ::y ::coord)
(s/def ::w ::coord)
(s/def ::h ::coord)
(s/def ::page-number pos-int-or-zero?)
(s/def ::area (s/keys :req-un [::id ::x ::y ::w ::h ::page-number]))

(defn parse-text [pdfdoc {:keys [version areas] :as cfg}]
  (log/info (str "Parsing " version " pdf bill"))
  (let [areas (filter #(-> % :id namespace (= version)) areas)
        texts (extract-by-areas pdfdoc areas)]
    (->> (map #(vector (:id %1) (str/trim %2)) areas texts)
         (into {}))))

(comment
  (require '[me.raynes.fs :as fs])
  (def areas (-> "coordinates.edn"
                 io/resource
                 slurp
                 edn/read-string))
  (def pdfdocv1 (fs/expand-home "~/.cache/delilah/dei/downloads/raw/testv1.pdf"))

  (def textv1 (parse-text (str pdfdocv1) {:version "v1"
                                          :areas   areas}))

  (def pdfdocv2 (fs/expand-home "~/.cache/delilah/dei/downloads/raw/testv2.pdf"))

  (def textv2 (parse-text pdfdocv2 {:version "v2"
                                    :areas   areas})))
