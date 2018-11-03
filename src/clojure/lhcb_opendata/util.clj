(ns lhcb-opendata.util
  (:require [clojure.string :as str]
            [ignition.sql
             [dataframe-stat-functions :as ds-stats]
             [dataset :as ds]
             [row :as row]
             [spark-session :as session]]
            [sparkling
             [function :as func]
             [serialization]]
            [sparkling.sql.types :as types])
  (:import [org.apache.spark.sql functions Column]))

(defn nbins-freedman-diaconis
  "Calculates the bins and bin width using Freedman-Diaconis rule.

  2 * IQR * n^{-1/2}

  IQR: Inter Quartile Range."
  [sess d col-name]
  (let [col (ds/col d col-name)
        [min-val max-val] (-> (ds/select d [(functions/min col) (functions/max col)])
                              (ds/take 1)
                              (first)
                              ((juxt #(row/get-as % (format "min(%s)" col-name))
                                     #(row/get-as % (format "max(%s)" col-name)))))
        [lower upper]     (-> (ds/stat d)
                              (ds-stats/approx-quantile col-name [0.25 0.75] 0.01))
        n                 (ds/count d)
        width  (/ (* 2 (- upper lower)) (Math/cbrt n))
        bins   (range min-val max-val width)]
    {:width width :bins bins}))

(defn histogram-bin
  "Returns the bin the value `x` falls within.

  As a hack it is returning `Double/MAX_VALUE` when the value does not fall
  within a bin. Keywords identifying below and above the range would be better
  but I couldn't get those to play well with Spark literals.

  If `x` is nil then nil is returned."
  [bins x]
  (cond
    (nil? x)           nil
    (< x (first bins)) Double/MAX_VALUE ;; ::below-min
    (>= x (last bins)) Double/MAX_VALUE ;; ::above-max
    :else              (reduce (fn [acc b]
                                 (if (and (<= acc x) (< x b))
                                   (reduced acc)
                                   b))
                               bins)))

(defn histogram-bin-udf
  "Returns a column which is the result of applying the `histogram-bin` function to a column."
  [bins col]
  (.apply (functions/udf (func/function (partial histogram-bin bins)) (types/double-type))
          (into-array Column [col])))

(defn sum-cols
  "Returns a new column which is the sum of all the columns in the sequence."
  [cols]
  (reduce (fn [sum col] (.plus sum col)) cols))

(defn sum-squares
  "Returns a new columnn which is the sum of the squares of all the columns in the input sequence."
  [cols]
  (sum-cols (map (fn [col] (functions/pow col 2.0)) cols)))

(defn with-momentum
  "Adds two new columns to the dataset of candidate's the momentum and momentum squared.

  The prefix is used to identify a candidate in the dataset and it's momentum
  components and name the output. For example, where the prefix is `H2_` the
  momentum is built from the x,y and z components and the returned columns are
  called `H2_P2` and `H2_P`.

  The prefix is used to identify the hadron in the dataset e.g. `H2_`. For
  example, where the provided prefix is `H2_` then the The total
  momentum column is called H2_P where H2_ is the "
  [ds col-prefix]
  (let [ps (map (fn [p] (ds/col ds (str col-prefix p))) ["PX" "PY" "PZ"])
        p2 (sum-squares ps)
        p  (functions/sqrt p2)]
    (-> ds
        (ds/with-column (str col-prefix "P2") p2)
        (ds/with-column (str col-prefix "P") p))))

(defn with-energy
  "Adds new columns for the candidate energy and energy square.

  The energy is calculated from the candidate momentum and the provided
  invariant mass column."
  [ds col-prefix mass]
  (let [p-cols (map (fn [p] (ds/col ds (str col-prefix p))) ["PX" "PY" "PZ"])
        e-sq   (sum-squares (cons mass p-cols))
        e      (functions/sqrt e-sq)]
    (-> ds
        (ds/with-column (str col-prefix "E2") e-sq)
        (ds/with-column (str col-prefix "E") e))))

(def kaon-mass-mev
  "Column representing the charged kaon mass as a literal."
  (functions/lit 493.677))

(defn with-b-mass
  "Adds a new column for the B^\\pm candidate mass plus the intermediate columns."
  [ds]
  (let [ds (-> ds
               (with-momentum "H1_")
               (with-momentum "H2_")
               (with-momentum "H3_")
               (with-energy "H1_" kaon-mass-mev)
               (with-energy "H2_" kaon-mass-mev)
               (with-energy "H3_" kaon-mass-mev))
        b-px (sum-cols (map (partial ds/col ds) ["H1_PX" "H2_PX" "H3_PX"]))
        b-py (sum-cols (map (partial ds/col ds) ["H1_PY" "H2_PY" "H3_PY"]))
        b-pz (sum-cols (map (partial ds/col ds) ["H1_PZ" "H2_PZ" "H3_PZ"]))
        b-e  (sum-cols (map (partial ds/col ds) ["H1_E" "H2_E" "H3_E"]))
        b-e2 (functions/pow b-e 2.0)]
    (-> ds
        (ds/with-column "B_PX" b-px)
        (ds/with-column "B_PY" b-py)
        (ds/with-column "B_PZ" b-pz)
        (with-momentum "B_")
        (ds/with-column "B_E" b-e)
        (ds/with-column "B_E2" b-e2)
        (ds/with-column "B_M" (functions/sqrt (.minus b-e2 (sum-squares [b-px b-py b-pz])))))))

(defn b-candidate-seletion
  "Quick selection to apply to the reconstructed B-candidate."
  [ds]
  (let [kprobs  (reduce (fn [acc prob] (.and acc prob))
                        (map (fn [cand] (.geq (ds/col ds (str cand "_ProbK")) 0.7))
                             ["H1" "H2" "H3"]))
        b_vchi2 (.geq (ds/col ds "B_VertexChi2") 0.7)]
    (.and b_vchi2 kprobs)))

(defn simulation
  [sess workspace]
  (ds/read-root sess (str workspace "PhaseSpaceSimulation.root")))

(defn magnet-down
  [sess workspace]
  (ds/read-root sess (str workspace "B2HHH_MagnetDown.root")))

(defn magnet-up
  [sess workspace]
  (ds/read-root sess (str workspace "B2HHH_MagnetUp.root")))
