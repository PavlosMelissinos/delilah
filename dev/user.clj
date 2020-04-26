(ns user
  (:require [clojure.tools.namespace.repl :as ns-tools]
            [expound.alpha :as expound]
    ;;[pyro.printer :as pyro]
            [clojure.spec.alpha :as s]))

(set! *warn-on-reflection* true)
(ns-tools/disable-unload!)


(defn expound-explain-out! []
  (alter-var-root #'s/*explain-out* (constantly expound/printer))
  (set! s/*explain-out* expound/printer))


(defn default-explain-out! []
  (alter-var-root #'s/*explain-out* (constantly s/explain-printer))
  (set! s/*explain-out* s/explain-printer))


(defn load-dev []
  ;;(pyro/swap-stacktrace-engine!)
  (expound-explain-out!)
  (s/check-asserts true)
  (require 'dev)
  (in-ns 'dev))


(defn dev []
  (require 'dev)
  (in-ns 'dev)
  nil)


(defn fix []
  (ns-tools/refresh-all))