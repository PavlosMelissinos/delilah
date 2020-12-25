(ns dev
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.namespace.repl :as ns-tools]
            [clojure.spec.test.alpha :as stest]
            [kaocha.repl :as kaocha]))

(def refresh ns-tools/refresh)
(def refresh-all ns-tools/refresh-all)

(defn instrument-all []
  (stest/instrument (stest/instrumentable-syms)))

(defn unstrument-all []
  (stest/unstrument (stest/instrumentable-syms)))

(defn run-all-my-tests []
  (instrument-all)
  (kaocha/run :unit))

(defn run-tests
  [& args]
  (if (empty? args)
    (kaocha/run *ns*)
    (apply kaocha/run args)))
