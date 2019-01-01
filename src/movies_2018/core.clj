(ns movies-2018.core
  (:require [clojure.data.csv :as csv]
            [clojure.string :as s]
            [clojure.java.io :as io]))

(defn to-record [[position const created modified description title url title-type
                  imdb-rating runtime year genres votes date directors your-rating _]]
  {:created created
   :watch-year (-> created (s/split #"-") first read-string)
   :title title
   :imdb-rating (read-string imdb-rating)
   :runtime (if (= "" runtime) 0 (read-string runtime))
   :year (read-string year)
   :genres (-> genres (s/split #", ") set)
   :votes (read-string votes)
   :date date
   :directors directors
   :your-rating (when-not (= "" your-rating) (read-string your-rating))})

(defmulti load-data identity)

(defmethod load-data :basic [_]
  (let [data (with-open [rdr (io/reader "./data/WATCHLIST.csv")]
               (doall (csv/read-csv rdr)))]
    (for [item (rest data)]
      (try
        (to-record item)
        (catch Exception e
          (println item))))))

(defn rating-difference [{:keys [imdb-rating your-rating]}]
  (when (every? number? [imdb-rating your-rating])
    (math/abs (- your-rating imdb-rating))))

(defmethod load-data :with-ratings [_]
  (->> (load-data :basic)
       (map #(assoc % :rating-difference (rating-difference %)))))
