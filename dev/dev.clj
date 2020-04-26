(ns dev
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.namespace.repl :as ns-tools]
            [clojure.test :as test]
            [clojure.spec.test.alpha :as stest]))

(def refresh ns-tools/refresh)
(def refresh-all ns-tools/refresh-all)

(defn instrument-all []
  (stest/instrument (stest/instrumentable-syms)))

(defn unstrument-all []
  (stest/unstrument (stest/instrumentable-syms)))

(defn run-all-my-tests []
  (instrument-all)
  (test/run-all-tests #"^monitor.+|bsq.+$"))
