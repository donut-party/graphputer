(ns donut.graphputer
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [clojure.data :as data]))

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

(def NodesSchema
  [:map-of :keyword [:map
                     [:pute fn?]
                     [:edges {:optional true} [:map-of :keyword :keyword]]
                     [:schemas {:optional true} :map]]])

(def GraphSchema
  [:map
   [:id :keyword]
   [:init :keyword]
   [:nodes NodesSchema]])

(defn validate-schema
  [node-name schemas schema-name value]
  (let [schema (schema-name schemas)]
    (when-let [explanation (and schema (m/explain schema value))]
      (throw (ex-info (str "Schema validation failed")
                      {:node-name          node-name
                       :schema-name        schema-name
                       :spec-explain-human (me/humanize explanation)
                       :spec-explain       explanation})))))

(defn validate-graph
  [graph]
  (when-let [explanation (m/explain GraphSchema graph)]
    (throw (ex-info "graph definition invalid" {:spec-explain-human (me/humanize explanation)
                                                :spec-explain explanation})))

  (when-not (get-in graph [:nodes (:init graph) :pute])
    (throw (ex-info ":init does not refer to valid node" (select-keys graph [:init]))))

  (let [defined-node-names                  (set (keys (:nodes graph)))
        referenced-node-names               (->> graph
                                                 :nodes
                                                 vals
                                                 (mapcat (comp vals :edges))
                                                 (into #{(:init graph)}))
        [unreachable-nodes undefined-nodes] (data/diff defined-node-names referenced-node-names)]

    (when (seq undefined-nodes)
      (throw (ex-info "node edges refer to undefined nodes" {:undefined-nodes undefined-nodes})))

    (when (seq unreachable-nodes)
      (throw (ex-info "nodes defined but no edges refer to them" {:unreachable-nodes unreachable-nodes})))))

(defn execute
  [{:keys [init nodes validate?]
    :or   {validate? true}
    :as   graph}
   ctx]
  (validate-graph graph)
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
