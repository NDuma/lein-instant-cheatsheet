(ns instant-cheatsheet.api
  "API endpoints that return some subset of the data defined in `instant-cheatsheet.sources`."
  (:require [cheshire.core :as cheshire]
            [compojure.core :refer [defroutes GET]]
            (instant-cheatsheet [sources :as sources]
                                [util :as util])))


(defmacro def-api-fn
  "Helper macro to create an API function that returns JSON"
  [name docstr args-form & body]
  `(defn ~name ~docstr ~args-form
     {:status 200
      :headers {"Content-Type" "application/json"}
      :body (cheshire/generate-string (do ~@body))}))

(def-api-fn get-matches
  "Return vector of [{:name symbol-name :matches [infodict+]}+] of symbols that match Q."
  [q]
  (->> (filter (fn [symbol-str] (.contains symbol-str q))
               @sources/all-symbols-keys)
       (take 50) ; reasonable limit <3
       (sort-by (partial util/levenshtein-distance q))
       (map (fn [match]
              {:name match
               :matches (@sources/all-symbols match)}))))

(def-api-fn get-source
  "Return the source code for symbol in specified namespace."
  [ns-name-str symb-name]
  (let [qualified-symbol (symbol ns-name-str symb-name)]
    {:source (util/source-for-symbol qualified-symbol)}))

(defroutes routes
  (GET "/matches" [q] (get-matches q))
  (GET "/source" [ns symbol] (get-source ns symbol)))