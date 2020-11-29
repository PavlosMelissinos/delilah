(ns delilah.gr.deddie.parser
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]

            [java-time :as t]

            [delilah.common.parser :as cparser]
            [delilah :as d]))

;; Time

(defn latin-timestr [time]
  (-> time
      clojure.string/upper-case
      (clojure.string/replace #"ΠΜ" "AM")
      (clojure.string/replace #"ΜΜ" "PM")))
(s/fdef latin-timestr
  :args (s/cat :time string?)
  :ret string?)

(defn str->datetime
    ([datetime]
     (str->datetime datetime "d/M/yyyy h:mm:ss a"))
    ([datetime formatter]
     (log/info (format "Coercing datetime string %s as %s" datetime formatter))
     (-> datetime
         latin-timestr
         (#(t/local-date-time formatter %)))))
(s/fdef str->datetime
  :args (s/and (s/cat :datetime string?)))

(defn datetime->str
  ([datetime]
   (datetime->str datetime "yyyy/M/dd hh:mm:ss"))
  ([datetime formatter]
   (t/format formatter datetime)))
(s/fdef datetime->str
  :args (s/and (s/cat :datetime t/local-date-time?)))

(defn str->time
  ([time]
   (str->time time "hh:mm a"))
  ([time formatter]
   (log/info (format "Coercing time string %s as %s" time formatter))
   (-> time
       latin-timestr
       (#(t/local-time formatter %)))))

(defn deaccent [str]
  "Remove accent from string"
  ;; http://www.matt-reid.co.uk/blog_post.php?id=69
  (let [normalized (java.text.Normalizer/normalize str java.text.Normalizer$Form/NFD)]
    (clojure.string/replace normalized #"\p{InCombiningDiacriticalMarks}+" "")))

(defn cleanup [{:keys [content] :as tbl-entry}]
  (log/info (str "Cleaning up table entry " tbl-entry))
  (when content (-> content first clojure.string/trim)))


(defn- split-area-text [area-text]
  (-> (str "affected-numbers\n" area-text)
      (clojure.string/replace "οδός:" "\nstreet\n")
      (clojure.string/replace "από:" "\nfrom\n")
      (clojure.string/replace "έως:" "\nto\n")
      (clojure.string/replace "απο κάθετο:" "\nfrom-street\n")
      (clojure.string/replace "έως κάθετο:" "\nto-street\n")
      (clojure.string/split-lines)
      ((partial map clojure.string/trim))))

(defn- parse-area-text [area-text]
  (->> area-text
       split-area-text
       (apply hash-map)
       walk/keywordize-keys))

(defn affected-area [area-text]
  (log/info "Parsing outage data for affected area...")
  (cond
    (or (str/starts-with? area-text "Μονά")
        (str/starts-with? area-text "Ζυγά"))
    (let [parse-times-fn (fn [time] (when time (str->time time "hh:mm a")))]
      (-> area-text
          parse-area-text
          (update :from #(parse-times-fn %))
          (update :to #(parse-times-fn %))))

    :else
    area-text))

(defn affected-areas [all-areas-text]
  (->> (clojure.string/split all-areas-text #"\n")
       (map affected-area)))

(defn outage [o]
  (log/info (str "Enriching outage reports"))
  (let [outage-map (zipmap [:start :end :municipality :affected-areas :note-id :cause] o)]
    (-> outage-map
        (update :start str->datetime)
        (update :end str->datetime)
        (assoc :affected-areas-raw (:affected-areas outage-map))
        (update :affected-areas affected-areas))))