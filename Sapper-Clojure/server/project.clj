(defproject sapper "0.1.0-SNAPSHOT"
  :description "Multiplayer Sapper Game on Clojure Server"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [http-kit "2.7.0"]
                 [compojure "1.7.0"]
                 [cheshire "5.11.0"]
                 [org.clojure/core.async "1.6.681"]
                 [ring/ring-defaults "0.3.4"]]   ; ← ЭТО ВАЖНО
  :main sapper.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all
                       :uberjar-name "sapper.jar"}})
