(ns delilah.deddie.gr.parser
  (:require [clojure.tools.logging :as log]

            [etaoin.api :as api]
            [hickory.select :as hs]

            [delilah.common.parser :as cparser]))

(defn filter-power-cuts-by
  ([county]
   (power-cuts county nil))
  ([county municipality]
   (log/info "Firing up deddie.gr outages page...")
   (doto driver
     (api/go "https://siteapps.deddie.gr/Outages2Public")
     (api/wait-visible {:id :PrefectureID})
     (api/wait-visible {:id :MunicipalityID}))

   (log/info (format "Navigating to outages for %s/%s..." county municipality))
   (api/click driver [{:tag :select :id :PrefectureID} {:tag :option :fn/text county}])
   (when municipality
     (api/wait-visible driver [{:tag :select :id :MunicipalityID} {:tag :option :fn/text municipality}])
     (api/click driver [{:tag :select :id :MunicipalityID} {:tag :option :fn/text municipality}]))
   (log/info "Got outage info!")
   driver))

(comment
  (def driver (api/firefox {:headless    false
                            :path-driver "resources/webdrivers/geckodriver"}))

  (filter-power-cuts-by "ΘΕΣΣΑΛΟΝΙΚΗΣ" "ΘΕΣΣΑΛΟΝΙΚΗΣ")
  (filter-power-cuts-by "ΑΤΤΙΚΗΣ" "ΠΕΡΙΣΤΕΡΙΟΥ")
  (def driver (filter-power-cuts-by "ΑΤΤΙΚΗΣ" "ΠΕΡΙΣΤΕΡΙΟΥ"))
  (def outages
    (let [outages-selector (hs/id "tblOutages")]
      (->> (api/get-source driver)
           cparser/parse
           (hs/select outages-selector))))

  (hs/select (or (hs/tag :th) (hs/tag :td)) outages)
  (hs/select (hs/descendant
              (or (hs/tag :thead) (hs/tag :tbody))
              (or (hs/tag :th) (hs/tag :td))) outages)
  (api/quit driver))
