(ns donut.graphputer)

(defn append-node
  [graph node-name node parent-name branch-name]
  (-> graph
      (assoc-in [:nodes node-name] node)
      (assoc-in [:nodes parent-name branch-name] node-name)))

(defn append-side-effect-node
  "appends a node and wires up :success so dev doesn't have to manage that. assumption
  is that `:pute` will always succeed because there are no control flow decisions"
  [graph node-name node parent-name branch-name]
  (let [existing-branch (get-in graph [:nodes parent-name branch-name])]
    (append-node graph
                 node-name
                 (assoc node :success existing-branch)
                 parent-name
                 branch-name)))

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
