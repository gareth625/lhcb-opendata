(ns lhcb-opendata
  (:gen-class)
  (:require [cheshire.core :as json]
            [cliopatra.command :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ignition.sql
             [dataframe-stat-functions :as ds-stats]
             [dataset :as ds]
             [row :as row]
             [spark-session :as session]]
            [lhcb-opendata.util :as util]
            [oz.core :as oz]
            [oz.server :as oz-server]
            [sparkling
             [conf :as conf]
             [core :as spark]
             [function :as func]
             [serialization]]
            [sparkling.sql.types :as types])
  (:import [org.apache.spark.sql functions Column]))

(defn ensure-trailing-slash
  [s]
  (if (.endsWith s "/") s (str s "/")))

(defn ensure-leading-slash
  [s]
  (if (.startsWith s "/") s (str "/" s)))

(defn plot-reconstructed-b-mass
  [sess ds filter?]
  (let [ds      (cond-> (util/with-b-mass ds)
                  filter? (ds/where (util/b-candidate-seletion ds)))
        b-mass (as-> ds bs
                 (ds/select bs [(ds/col bs "B_M")])
                 (ds/take bs (ds/count bs))
                 (map (fn [row] {:b-mass (row/get-as row "B_M")}) bs))
        binning (util/nbins-freedman-diaconis sess ds "B_M")
        plot    {:data     {:values b-mass}
                 :encoding {:x {:field "b-mass" :bin {:step (:width binning)}}
                            :y {:aggregate "count"}}
                 :mark     "bar"}]
    plot))

(cli/defcommand lhcb-opendata
  "Work through the LHCb open dataset answering the problems in Clojure and Spark."
  {:opts-spec
   [["--workspace"
     "The location of the input datasets and where to write the output to."
     :required true
     :parse-fn ensure-trailing-slash]
    ["--dataset"
     "The dataset to process, either 'simulation', 'magnet-down' or 'magnet-up'."
     :required true
     :parse-fn keyword]
    ["--master" "Override the Spark master."]
    ["--[no-]filter-selection"
     "Switch. When present applies the selection criteria to the B-candidate."
     :default false
     :required false]]}
  (let [spark-conf {"spark.sql.shuffle.partitions"  "2"
                    "spark.default.parallelism"     "2"
                    "spark.driver.cores"            "1"
                    "spark.driver.memory"           "1G"
                    "spark.driver.memoryOverhead"   "100M"
                    "spark.executor.cores"          "1"
                    "spark.executor.memory"         "1G"
                    "spark.executor.memoryOverhead" "100M"}
        sess-conf  (cond-> {:app-name "lhcb-opendata" :config spark-conf}
                     master (assoc :master master))]
    (with-open [sess (session/spark-session sess-conf)
                out  (io/writer (str workspace "b-mass-plot.json"))]
      (let [data (case dataset
                   :simulation  (util/simulation sess workspace)
                   :magnet-down (util/magnet-down sess workspace)
                   :magnet-up   (util/magnet-up sess workspace))
            plot (plot-reconstructed-b-mass sess data filter-selection)]
        (json/generate-stream plot out)))))

(defn -main
  [& args]
  (cli/dispatch 'lhcb-opendata args))


(comment
  (-main "lhcb-opendata"
         "--workspace"   "data"
         "--dataset"     "simulation"
         "--master"      "local[1]"
         "--no-filter-selection")

  ;; Good configuration makes a difference
  (def spark-session
    (session/spark-session {:app-name "lhcb-opendata-repl"
                            :master   "local[1]"
                            :config   {"spark.driver.cores"            "1"
                                       "spark.driver.memory"           "1G"
                                       "spark.driver.memoryOverhead"   "100M"
                                       "spark.executor.cores"          "1"
                                       "spark.executor.memory"         "1G"
                                       "spark.executor.memoryOverhead" "100M"
                                       "spark.sql.shuffle.partitions"  "1"
                                       "spark.default.parallelism"     "1"}}))
  (.close spark-session)

  (println (session/version spark-session))


  (oz/start-plot-server!)
  (oz-server/stop!)

  (ds/show (simulation spark-session "data/") 1 0 true)

  (let [data        (util/simulation spark-session "data/")
        b-mass-plot (plot-reconstructed-b-mass spark-session data)]
    (println (take 10 (:data b-mass-plot)))
    (oz/v! (assoc-in b-mass-plot [:encoding :x :bin] true)))

  ;; Without cuts
  (let [data        (ds/sample (util/magnet-down spark-session "data/") 0.10)
        b-mass-plot (plot-reconstructed-b-mass spark-session data false)]
    (println (take 10 (:data b-mass-plot)))
    (oz/v! b-mass-plot))

  ;; With cuts
  (let [data        (ds/sample (util/magnet-down spark-session "data/") 0.2)
        b-mass-plot (plot-reconstructed-b-mass spark-session data true)]
    (println (take 10 (:data b-mass-plot)))
    (oz/v! b-mass-plot))


  ;; Lets stream
  ;; First create a CSV we can sent to a socket.
  (-> (util/simulation spark-session "data/")
      (ds/write-json "data/simulation.json"))
  (-> (util/simulation spark-session "data/")
      (ds/print-schema))


  (def running-query (atom nil))
  (let [source-data (.. (.readStream spark-session)
                        (format "socket")
                        (option "host" "localhost")
                        (option "port" "9999")
                        (load))
        query       (.. source-data
                        (writeStream)
                        (outputMode "append")
                        (format "console")
                        (start))]
    (reset! running-query query)
    (future (.awaitTermination query)))

  (let [schema      (types/struct-type [{:name      "B_FlightDistance"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "B_VertexChi2"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_PX"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_PY"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_PZ"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_ProbK"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_ProbPi"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_Charge"
                                         :type      (types/integer-type)
                                         :nullable? true}
                                        {:name      "H1_IPChi2"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H1_isMuon"
                                         :type      (types/integer-type)
                                         :nullable? true}
                                        {:name      "H2_PX"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_PY"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_PZ"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_ProbK"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_ProbPi"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_Charge"
                                         :type      (types/integer-type)
                                         :nullable? true}
                                        {:name      "H2_IPChi2"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H2_isMuon"
                                         :type      (types/integer-type)
                                         :nullable? true}
                                        {:name      "H3_PX"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_PY"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_PZ"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_ProbK"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_ProbPi"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_Charge"
                                         :type      (types/integer-type)
                                         :nullable? true}
                                        {:name      "H3_IPChi2"
                                         :type      (types/double-type)
                                         :nullable? true}
                                        {:name      "H3_isMuon"
                                         :type      (types/integer-type)
                                         :nullable? true}])
        source-data (.. (.readStream spark-session)
                        (schema schema)
                        (json "data/sim/*.json"))
        query       (.. (util/with-momentum source-data "H1_")
                        (writeStream)
                        (outputMode "append")
                        (format "console")
                        (start))]
    (reset! running-query query)
    (future (.awaitTermination query)))

  (swap! running-query (fn [q] (.stop q)))




  )
