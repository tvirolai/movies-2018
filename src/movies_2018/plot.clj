(ns movies-2018.plot
  (:require [movies-2018.core :as core]
            [movies-2018.stats :as stats]
            [oz.core :as oz]))

(defn bars [data]
  {:data {:values (->> data (filter (partial stats/seen-in? 2018)) stats/genre-freq-map)}
   :encoding {:x {:field "genre" :type "ordinal"}
              :y {:field "quantity" :type "quantitative"}
              :color {:field "genre" :type "nominal" :legend nil}}
   :mark "bar"})

(defn month-histogram [data]
  {:data {:values (stats/month-counts data 2018)}
   :encoding {:x {:field "month" :type "ordinal"}
              :y {:field "quantity" :type "quantitative"}}
   :title "Films seen by month"
   :mark "bar"})

(defonce start
  (oz/start-plot-server!))
