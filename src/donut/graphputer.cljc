(ns donut.graphputer)

;; TODO exception handling
;; TODO schemas for pre- and post- ctx

(defn splice-node
  ([graph node-name node parent-name]
   (splice-node graph node-name node parent-name :success))
  ([graph node-name node parent-name branch-name]
   (let [existing-branch (get-in graph [:nodes parent-name branch-name])]
     (-> graph
         (assoc-in [:nodes node-name]
                   (update node :success #(or % existing-branch)))
         (assoc-in [:nodes parent-name branch-name] node-name)))))

(defn execute
  [{:keys [init nodes] :as _graph} ctx]
  (loop [node-name init
         ctx ctx]
    (let [{:keys [pute success fail]} (node-name nodes)
          result (pute ctx)]
      (cond
        (= result ::fail)
        (recur fail ctx)

        (and (sequential? result)
             (= ::fail (first result)))
        (recur fail (second result))

        success
        (recur success result)

        :else result))))
