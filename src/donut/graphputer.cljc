(ns donut.graphputer)

;; TODO exception handling
;; TODO schemas for pre- and post- ctx

(defn splice-node
  [graph
   {:keys [node-name node input-node-name input-branch-name output-branch-name]
    :or   {input-branch-name  :default
           output-branch-name :default}}]
  (let [existing-branch (get-in graph [:nodes input-node-name input-branch-name])]
    (-> graph
        (assoc-in [:nodes node-name]
                  (update node output-branch-name #(or % existing-branch)))
        (assoc-in [:nodes input-node-name input-branch-name] node-name))))

(defn execute
  [{:keys [init nodes] :as _graph} ctx]
  (loop [node-name init
         ctx       ctx]
    (let [{:keys [pute success] :as node} (node-name nodes)
          result                          (pute ctx)
          [goto? branch-name new-ctx]     (when (and (sequential? result)
                                                     (= ::goto (first result)))
                                            result)]
      (cond
        goto?
        (recur (branch-name node) new-ctx)

        success
        (recur success result)

        :else result))))
