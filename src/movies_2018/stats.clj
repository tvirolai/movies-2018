(ns movies-2018.stats
  (:require [movies-2018.core :as core]
            [kixi.stats.core :as kixi]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [oz.core :as oz]))

(defn seen-in? [year movie]
  (-> movie :watch-year (= year)))

(defn explain-time [mins]
  (let [hours (/ mins 60.0)]
    {:min mins
     :hours hours
     :days (/ hours 24.0)}))

(defn length-counts-in [year data]
  (transduce (comp (filter (partial seen-in? year)) (map :runtime)) + data))

(defn lengths-by-year [data]
  (for [year (range 2012 2019)]
    {:year year
     :lengths (length-counts-in year data)}))

(defn genre-freqs [data]
  (->> data
       (map (comp vec :genres))
       flatten
       frequencies
       (sort-by val)
       reverse))

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
       (filter #(not= nil (:rating-difference %)))
       (sort-by :rating-difference)
       reverse))

(defn month-counts [data year]
  (let [d (filter (partial seen-in? year) data)]
    (->> d
         (map (comp second #(s/split % #"-") :created))
         frequencies
         (map (fn [[k v]] {:month k :quantity v}))
         (sort-by :month))))

(defn rating-correlation [data]
  (transduce identity (kixi/correlation :imdb-rating :your-rating) data))

(defn films-by-month [data]
  (mapcat (fn [year]
            (->> (month-counts data year)
                 (map #(assoc % :year year))))
          (range 2013 2019)))

