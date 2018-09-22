(ns sparkling.sql.function)

(defn map-function
  [f]
  (new sparkling.function.MapFunction f))
