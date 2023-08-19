(ns donut.graphputer)

;; TODO exception handling
;; TODO schemas for pre- and post- ctx

(defn splice-node
  [graph
   {:keys [node-name node input-node-name input-edge-name output-edge-name]
    :or   {input-edge-name  :default
           output-edge-name :default}}]
  (let [existing-edge (get-in graph [:nodes input-node-name :edges input-edge-name])]
    (-> graph
        (assoc-in [:nodes node-name]
                  (update-in node [:edges output-edge-name] #(or % existing-edge)))
        (assoc-in [:nodes input-node-name :edges input-edge-name]
                  node-name))))

(defn execute
  [{:keys [init nodes] :as _graph} ctx]
  (loop [node-name init
         ctx       ctx]
    (let [{:keys [pute edges] :as node} (node-name nodes)
          result                          (pute ctx)
          [goto? edge-name new-ctx]     (when (and (sequential? result)
                                                   (= ::goto (first result)))
                                          result)]
      (cond
        goto?
        (recur (get-in node [:edges edge-name]) new-ctx)

        (:default edges)
        (recur (:default edges) result)

        :else result))))
