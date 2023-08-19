(ns examples
  (:require
   [donut.graphputer :as puter]))

(defn user-signup
  [params]
  (if-let [validation-errors (validate params)]
    (if-let [user (insert-user params)]
      {:status 200
       :body   user}
      {:status 500})
    {:status 400
     :body validation-errors}))


(def user-signup-graph
  {:id   :user-signup
   :init :validate
   :nodes
   {:validate
    {:pute  (fn [user-params]
              (if-let [validation-errors (validate user-params)]
                [::puter/goto :fail validation-errors]
                user-params))
     ;; the vector [::puter/goto :fail new-parameter] tells graphputer to follow
     ;; the `:fail` edge and to pass in `new-parameter` to that node's `:pute`
     :edges {:default :insert-user
             :fail    :validate-failed}}

    :validate-failed
    {:pute (fn [validation-errors]
             {:status 400
              :body   validation-errors})}

    :insert-user
    {:pute  (fn [user-params]
              (if-let [inserted-user (insert-user user-params)]
                inserted-user
                [::puter/goto :fail]))
     :edges {:default :user-signup-success
             :fail    :insert-user-failed}}

    :insert-user-failed
    {:pute (constantly {:status 500})}

    :user-signup-success
    {:pute (fn [inserted-user]
             {:status 200
              :body   inserted-user})}}})

(puter/execute user-signup-graph {:username "newuser"})

(def my-user-signup-graph
  (puter/splice-node
   user-signup-graph
   {:node-name       :email-user-signup-success
    :node            {:pute (fn [inserted-user] (email-user inserted-user))}
    :input-node-name :insert-user}))
