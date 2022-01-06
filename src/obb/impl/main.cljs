(ns obb.impl.main
  (:refer-clojure :exclude [slurp])
  (:require [clojure.core :as clojure]
            [clojure.tools.cli :as cli]
            [obb.impl.sci :as impl.sci]
            [sci.core :as sci]
            [sci.impl.interop :as interop]))

(def cli-options
  [["-e" "--eval <expr>"]])

(def app
  (delay (let [app (js/Application.currentApplication)]
           (set! (.-includeStandardAdditions app) true)
           app)))

(defn slurp
  [x]
  (.read @app (js/Path x #js {})))

(defn object-specifier?
  "Returns true if x is an object specifier."
  [x]
  (js/ObjectSpecifier.hasInstance x))

(defn not-object-specifier-pred-1 [f]
  (fn [x]
    (when-not (object-specifier? x)
      (f x))))

(defn not-object-specifier-pred-2 [f]
  (fn [x y]
    (when-not (object-specifier? x)
      (f x y))))

(set! interop/invoke-instance-method impl.sci/invoke-instance-method)

(set! interop/invoke-static-method impl.sci/invoke-static-method)

(set! clojure/map? (not-object-specifier-pred-1 map?))

(set! clojure/meta (not-object-specifier-pred-1 meta))

(enable-console-print!)

(sci/alter-var-root sci/print-fn (constantly *print-fn*))

(def ctx (sci/init {:classes {'js goog/global
                              :allow :all}}))

(defn eval-string
  [s]
  (sci/eval-string* ctx s))

(defn main [argv]
  (let [args (js->clj argv)
        {:keys [arguments summary] {form :eval} :options} (cli/parse-opts args cli-options)]
    (cond (some? form)
          (eval-string form)

          (and (seq arguments)
               (= 1 (count arguments)))
          (let [form (slurp (first arguments))]
            (eval-string form))

          :else
          (println summary))))
