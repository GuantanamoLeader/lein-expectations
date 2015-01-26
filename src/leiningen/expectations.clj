(ns leiningen.expectations
  (:import (java.io File))
  (:require [leiningen.core.main]))

(def ^:dynamic *exit-after-tests* true)

(defn eval-in-project
  "Support eval-in-project in both Leiningen 1.x and 2.x."
  [project form init]
  (let [[eip two?] (or (try (require 'leiningen.core.eval)
                            [(resolve 'leiningen.core.eval/eval-in-project)
                             true]
                            (catch java.io.FileNotFoundException _))
                       (try (require 'leiningen.compile)
                            [(resolve 'leiningen.compile/eval-in-project)]
                            (catch java.io.FileNotFoundException _)))]
    (if two?
      (eip project form init)
      (eip project form nil nil init))))

(defn namespaces-in-dir
  "Support namespaces-in-dir in both Leiningen 1.x and 2.x."
  [dir]
  (let [nid (or (try (require 'leiningen.util.ns)
                            (resolve 'leiningen.util.ns/namespaces-in-dir)
                            (catch java.io.FileNotFoundException _))
                       (try (require 'bultitude.core)
                            (resolve 'bultitude.core/namespaces-in-dir)
                            (catch java.io.FileNotFoundException _)))]
    (nid dir)))

(defn matching-ns?
  [to-match]
  (let [to-match (map re-pattern to-match)]
    (fn [ns]
      (if (empty? to-match)
        ns
        (->> (for [m to-match]
               (re-matches m (name ns)))
             (some identity))))))

(defn print-finished-ns [a-ns-name]
  (println "\n" a-ns-name "complete"))

(defn print-finished-expectation [_]
  (print ".")
  (flush))

(defn expectations
  "Executes expectation tests in your project.
   By default all test namespaces will be run, or you can specify
   which namespaces to run using regex syntax to filter."
  [project & args]
  (let [paths (if (:test-path project)
                [(:test-path project)]
                (:test-paths project))
        ns (->> (mapcat namespaces-in-dir paths)
                (filter (matching-ns? args)))
        results (doto (File/createTempFile "lein" "result") .deleteOnExit)
        path (.getAbsolutePath results)
        show-finished-ns (:expectations/show-finished-ns project)
        show-finished-expectation (:expectations/show-finished-expectation project)]
    (eval-in-project
     project
     `(do
        (expectations/disable-run-on-shutdown)
	 (let [cols# ["Name" "Status" "Info"]
	       fb# (clojure.java.io/writer (clojure.java.io/file "test_report.html"))]
	  (expectations/set-formatter (html/->HTMLFormatter fb# cols#) html/html-key))

        (doseq [n# '~ns]
          (require n# :reload))
        (binding [expectations/ns-finished ~(if show-finished-ns
                                              'leiningen.expectations/print-finished-ns
                                              'expectations/ns-finished)
                  expectations/expectation-finished ~(if show-finished-expectation
                                                       'leiningen.expectations/print-finished-expectation
                                                       'expectations/expectation-finished)]
          (expectations/run-all-tests))
        (shutdown-agents))
     '(require ['expectations ]
               ['expectations.formatters.html :as 'html]))
    (leiningen.core.main/abort "All test are done.")))
