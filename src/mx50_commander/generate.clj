(ns mx50-commander.generate
  "Generate iterations of parameters for a given command or commands."
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


(defn ^:private generate-steps
  "Returns the appropriate value from the collection for the given step out of
   num-steps, as calculated by the generator function which takes num-steps,
   step, and a collection of values."
  [generator num-steps collections]
  (vec (for [s (range num-steps)]
            (vec (for [c collections] (generator num-steps s (vec c)))))))


(defn ^:private filter-bindings
  [matcher bindings]
  (->> bindings
       (partition 2)
       (map vec)
       (filter (comp matcher first))
       (into (sorted-map))))


(defn curve
  "|------------|-------------------------------------|
   |num-steps   | 1 2 3 ...                           |
   |collections | ex. (range 50) [:a :b :c] \"static\"|

   Returns the appropriate value from the collection corresponding to a
   quadratic progression of steps."
  [num-steps & collections]
  (generate-steps get-curve num-steps collections))


(defn linear
  "|------------|-------------------------------------|
   |num-steps   | 1 2 3 ...                           |
   |collections | ex. (range 50) [:a :b :c] \"static\"|

   Returns the appropriate value from the collection corresponding to a linear
   progression of steps."
  [num-steps & collections]
  (generate-steps get-linear num-steps collections))


(defmacro generate
  ;; TODO docs
  "|----------|-------------------|
   | bindings | see below         |
   | body     | any clojure forms |

   ```
   (generate [color  [:red :blue :green]
              gain   (range 127 255)
              level  (range 255)
              :type  linear]
     (my-device (back-color-preset color gain))
     (my-device (fade-level level)))
   ```
   "
  [bindings & body]
  (let [gen-config (filter-bindings keyword? bindings)
        cmd-params (filter-bindings symbol? bindings)
        cmd-keys (keys cmd-params)
        cmd-vals (vec (vals cmd-params))
        generator (or (:type gen-config) 'mx50-commander.generate/linear)]
    `(let [steps# (~generator (count (first ~cmd-vals)) ~@cmd-vals)]
       (doseq [s# steps#]
         (apply (fn ~(vec cmd-keys) ~@body) s#)))))
