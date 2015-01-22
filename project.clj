(defproject org.clojars.khoda.lein-expectations "0.0.9"
  :description "Leiningen plugin to run tests written using the expectations library."
  :url "https://github.com/gar3thjon3s/lein-expectations"
  :dependencies [[org.clojars.khoda.expectations "2.0.13"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :expectations/show-finished-ns true
  :expectations/show-finished-expectation true
  :eval-in-leiningen true)
