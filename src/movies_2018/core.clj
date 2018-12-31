(ns movies-2018.core
  (:require [kixi.stats.core :as kixi]
            [clojure.data.csv :as csv]
            [clojure.string :as s]
            [clojure.math.numeric-tower :as math]
            [clojure.java.io :as io]
            [oz.core :as oz]))

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

(defn load-data []
  (let [data (with-open [rdr (io/reader "./data/WATCHLIST.csv")]
               (doall (csv/read-csv rdr)))]
    (for [item (rest data)]
      (try
        (to-record item)
        (catch Exception e
          (println item))))))

(defn seen-in? [year movie]
  (-> movie :watch-year (= year)))

(defn explain-time [mins]
  (let [hours (/ mins 60.0)]
    {:min mins
     :hours hours
     :days (/ hours 24.0)}))

(defn length-counts-in [year data]
  (transduce (comp (filter (partial seen-in? 2018)) (map :runtime)) + data))

(defn genre-freqs [data]
  (->> data
       (map (comp vec :genres))
       flatten
       frequencies
       (sort-by val)
       reverse))

(defn month-counts [data year]
  (let [d (filter (partial seen-in? year) data)]
    (->> d
         (map (comp second #(s/split % #"-") :created))
         frequencies
         (map (fn [[k v]] {:month k :quantity v}))
         (sort-by :month))))

(defn rating-difference [{:keys [imdb-rating your-rating]}]
  (when (every? number? [imdb-rating your-rating])
    (math/abs (- your-rating imdb-rating))))

(defn add-rating-difference [item]
  (assoc item :rating-difference (rating-difference item)))

(def month-histogram
  {:data {:values (month-counts (load-data) 2018)}
   :encoding {:x {:field "month" :type "ordinal"}
              :y {:field "quantity" :type "quantitative"}}
   :title "Films seen by month"
   :mark "bar"})

(defn top-rated [data year]
  (let [d (filter (partial seen-in? year) data)]
    (->> d
         (filter #(not= nil (:your-rating %)))
         (sort-by :your-rating)
         reverse)))

(defn genre-freq-map [data]
  (for [[k v] (genre-freqs data)]
    {:genre k :quantity v}))

(defn top-rating-difference [data year]
  (->> data
       (filter (partial seen-in? 2018))
       (map add-rating-difference)
       (filter #(not= nil (:rating-difference %)))
       (sort-by :rating-difference)
       reverse))

(defn rating-correlation [data]
  (let [d (map add-rating-difference data)]
    (transduce identity (kixi/correlation :imdb-rating :your-rating) d)))

(def bars
  {:data {:values (->> (load-data) (filter (partial seen-in? 2018)) genre-freq-map)}
   :encoding {:x {:field "genre" :type "ordinal"}
              :y {:field "quantity" :type "quantitative"}
              :color {:field "genre" :type "nominal" :legend nil}}
   :mark "bar"})

(defonce start
  (oz/start-plot-server!))
