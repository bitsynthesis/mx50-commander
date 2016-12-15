(ns mx50-commander.generators
  (:import java.lang.Math))


(defn ^:private get-curve
  [num-steps step collection]
  (let [max-index (- num-steps 1)]
    (if (zero? step)
      (collection step)
      (let [i (Math/pow (inc step)
                        (/ (Math/log (dec (count collection)))
                           (Math/log num-steps)))]
        (-> i
            float
            Math/round
            collection)))))


(defn ^:private get-linear
  [num-steps step collection]
  (if (zero? step)
    (collection step)
    (-> step
        (/ (dec num-steps))
        (* (dec (count collection)))
        float
        Math/round
        collection)))


(defn ^:private generate
  "Returns the appropriate value from the collection for the given step out of
   num-steps, as calculated by the generator function which takes num-steps,
   step, and a collection of values."
  [generator num-steps collections]
  (for [s (range num-steps)]
       (for [c collections]
            (if (sequential? c)
              (generator num-steps s (vec c))
              c))))


;; TODO make private
(defn filter-bindings
  [matcher bindings]
  (->> bindings
       (partition 2)
       (map vec)
       (filter (comp matcher first))
       (into (sorted-map))))


(defn re-bind [cmd-params step]
  (into [] (apply concat (map vector (keys cmd-params) step))))


(defmacro do-generate
  [bindings & body]
  ;; filter symbol bindings from keyword keys
  (let [gen-config (filter-bindings keyword? bindings)
        cmd-params (filter-bindings symbol? bindings)
        generator (:type gen-config)]

    ;; generate values of all symbol bindings by type

    `(for [step# (~generator ~(:steps gen-config) ~@(vals cmd-params))]
          (let [step-keys# ~(map name (keys @cmd-params))]
            (println step#)))




    ;; (println "GENERATIN")
    ;; (list 'let [steps# (list 'apply
    ;;                             (list 'partial generator
    ;;                                   (:steps gen-config))
    ;;                             (cons 'list (vals cmd-params)))]
    ;;       (list 'for [i (range (count steps#))]
    ;;             [(keys cmd-params) (get steps# i)]))
    ;;

    ;; for each step
    ;;   associate the step values with original binding symbols
    ;;   execute the body with those bindings

    ))


(defn curve
  "|------------|-------------------------------------|
   |num-steps   | 1 2 3 ...                           |
   |collections | ex. (range 50) [:a :b :c] \"static\"|

   Returns the appropriate value from the collection corresponding to a
   quadratic progression of steps."
  [num-steps & collections]
  (generate get-curve num-steps collections))


(defn linear
  "|------------|-------------------------------------|
   |num-steps   | 1 2 3 ...                           |
   |collections | ex. (range 50) [:a :b :c] \"static\"|

   Returns the appropriate value from the collection corresponding to a linear
   progression of steps."
  [num-steps & collections]
  (println "THEY CALLIN ME")
  (generate get-linear num-steps collections))
