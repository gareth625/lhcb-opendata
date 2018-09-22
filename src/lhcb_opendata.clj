(ns lhcb-opendata
  (:gen-class)
  (:require [cliopatra.command :as cli]
            [lhcb-opendata.sql.session :as session]
            [sparkling
             [conf :as conf]
             [core :as spark]
             [serialization]]
            [sparkling.sql.types :as types]))

(defn ensure-trailing-slash
  [s]
  (if (.endsWith s "/") s (str s "/")))

(defn ensure-leading-slash
  [s]
  (if (.startsWith s "/") s (str "/" s)))

(cli/defcommand lhcb-opendata
  "Work through the LHCb open dataset answering the problems in Clojure and Spark."
  {:opts-spec
   [["--workspace"
     "The location of the input datasets and where to write the output to."
     :required true
     :parse-fn ensure-trailing-slash]
    ["--master" "Override the Spark master."]]}
  (let [spark-conf {"spark.sql.shuffle.partitions" "2"
                    "spark.default.parallelism"    "2"}
        sess-conf  (cond-> {:app-name "lhcb-opendata" :config spark-conf}
                     master (assoc :master master))]
    (with-open [sess (session/spark-session sess-conf)]
      (println (session/version sess)))))

(defn -main
  [& args]
  (cli/dispatch 'lhcb-opendata args))

(comment
  (-main "lhcb-opendata"
         "--workspace"   "data"
         "--master"      "local[*]")

  )
