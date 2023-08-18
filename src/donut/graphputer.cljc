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
         ctx       ctx]
    (let [{:keys [pute success] :as node} (node-name nodes)
          result                          (pute ctx)
          [goto? branch-name new-ctx]     (when (and (sequential? result)
                                                     (= ::goto (first result)))
                                            result)]
      (prn goto? branch-name new-ctx)
      (cond
        goto?
        (recur (branch-name node) new-ctx)

        success
        (recur success result)

        :else result))))
