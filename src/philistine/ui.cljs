(ns philistine.ui
  (:require [reagent.dom :as rd]
            [re-frame.core :as rf]))

(enable-console-print!)

(prn "hello world!")

(defn header []
  [:div "HEADER"])

(defn footer []
  [:div "FOOTER"])

(defn app []
  [:div
   [header]
   [:main "MAIN"]
   [footer]])

(defonce init-event (rf/dispatch-sync [:ui/init]))

(rd/render [app] (.getElementById js/document "app"))
