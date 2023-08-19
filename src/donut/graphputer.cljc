(ns donut.graphputer
  (:require
   [malli.core :as m]
   [malli.error :as me]))

;; TODO splice-graph
;; - TODO handle name collisions? throw exception if there's a collision?

(defn splice-node
  "inserts a node in the graph, re-wiring edges"
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

(defn validate-schema
  [node-name schemas schema-name value]
  (let [schema (schema-name schemas)]
    (when-let [explanation (and schema (m/explain schema value))]
      (throw (ex-info (str "Schema validation failed")
                      {:node-name          node-name
                       :schema-name        schema-name
                       :spec-explain-human (me/humanize explanation)
                       :spec-explain       explanation})))))

(defn execute
  [{:keys [init nodes validate?]
    :or   {validate? true}
    :as   _graph}
   ctx]
  (loop [node-name init
         ctx       ctx]
    (let [{:keys [pute edges schemas]} (node-name nodes)]
      (when validate? (validate-schema node-name schemas ::input ctx))
      (let [result                    (pute ctx)
            [goto? edge-name new-ctx] (when (and (sequential? result)
                                                 (= ::goto (first result)))
                                        result)]
        (cond
          goto?
          (do
            (when validate? (validate-schema node-name schemas edge-name new-ctx))
            (recur (edge-name edges) new-ctx))

          (:default edges)
          (do
            (when validate? (validate-schema node-name schemas :default result))
            (recur (:default edges) result))

          :else
          (do
            (when validate? (validate-schema node-name schemas ::output result))
            result))))))
