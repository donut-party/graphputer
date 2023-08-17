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
    {:pute    (fn [user-params]
                (if-let [validation-errors (validate user-params)]
                  [::puter/fail validation-errors]
                  user-params))
     ;; the vector [::puter/fail ...] tells graphputer to follow
     ;; the :fail branch
     :success :insert-user
     :fail    :validate-failed}

    :validate-failed
    {:pute (fn [validation-errors]
             {:status 400
              :body   validation-errors})}

    :insert-user
    {:pute    (fn [user-params]
                (if-let [inserted-user (insert-user user-params)]
                  inserted-user
                  ::puter/fail)) ;; you can also use just ::puter/fail to go to fail branch
     :success :user-signup-success
     :fail    :insert-user-failed}

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
   :email-user-signup-success
   {:pute (fn [inserted-user] (email-user inserted-user))}
   :insert-user))
