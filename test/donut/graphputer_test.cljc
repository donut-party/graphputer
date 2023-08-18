(ns donut.graphputer-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [donut.graphputer :as puter]))

(deftest test-execute-all-default
  (is (= {:status 200
          :body   {:username "newuser"}}
         (puter/execute
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    (fn [ctx] ctx)
             :default :insert-user}

            :insert-user
            {:pute    (fn [ctx] ctx)
             :default :user-signup-default}

            :user-signup-default
            {:pute (fn [ctx]
                     {:status 200
                      :body   ctx})}}}
          {:username "newuser"}))))

(deftest test-execute-goto-no-context
  (is (= :validation-failed
         (puter/execute
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    (fn [_] [::puter/goto :fail])
             :default :insert-user
             :fail    :validate-failure}

            :validate-failure
            {:pute (fn [_] :validation-failed)}}}
          {}))))

(deftest test-execute-goto-with-new-context
  (is (= :new-context
         (puter/execute
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    (fn [_] [::puter/goto :fail :new-context])
             :default :insert-user
             :fail    :validate-failure}

            :validate-failure
            {:pute (fn [ctx] ctx)}}}
          {}))))


(deftest test-splice-node
  (is (= {:id   :user-signup
          :init :validate
          :nodes
          {:validate
           {:pute    identity
            :default :validate-success}

           ;; this is spliced in in between :validate and :insert-user
           :validate-success
           {:pute    identity
            :default :insert-user}

           :insert-user
           {:pute    identity
            :default :user-signup-default}}}
         (puter/splice-node
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute    identity
             :default :insert-user}

            :insert-user
            {:pute    identity
             :default :user-signup-default}}}
          {:node-name         :validate-success
           :node              {:pute identity}
           :input-node-name   :validate
           :input-branch-name :default}))))
