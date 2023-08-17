(ns donut.graphputer-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [donut.graphputer :as puter]))

;; conveying context
;; conveying context on failure
;; exception handling
;; schemas for pre- and post- ctx
;; helper to add side-effecting behavior?

(deftest test-execute-all-success
  (is (= {:status 200
          :body   {:username "newuser"}}
         (puter/execute
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    (fn [ctx] ctx)
             :success :insert-user}

            :insert-user
            {:pute    (fn [ctx] ctx)
             :success :user-signup-success}

            :user-signup-success
            {:pute (fn [ctx]
                     {:status 200
                      :body   ctx})}}}
          {:username "newuser"}))))

(deftest test-execute-keyword-fail
  (is (= :validation-failed
         (puter/execute
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    (fn [_] ::puter/fail)
             :success :insert-user
             :fail    :validate-failure}

            :validate-failure
            {:pute (fn [_] :validation-failed)}}}
          {}))))


(deftest append-side-effect-node
  (is (= {:id   :user-signup
          :init :validate
          :nodes
          {:validate
           {:pute    identity
            :success :validate-success}

           :validate-success
           {:pute identity
            :success :insert-user}

           :insert-user
           {:pute    identity
            :success :user-signup-success}}}
         (puter/append-side-effect-node
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    identity
             :success :insert-user}

            :insert-user
            {:pute    identity
             :success :user-signup-success}}}

          :validate-success
          {:pute identity}

          :validate
          :success))))
