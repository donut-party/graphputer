(ns donut.graphputer-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [donut.graphputer :as puter]))

(deftest test-execute-all-default
  (is (= {:status 200
          :body   {:username "newuser"}}
         (puter/execute
          {:id   :user-signup
           :init :validate

           :nodes
           {:validate
            {:pute  (fn [ctx] ctx)
             :edges {:default :insert-user}}

            :insert-user
            {:pute  (fn [ctx] ctx)
             :edges {:default :user-signup-default}}

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
            {:pute  (fn [_] [::puter/goto :fail])
             :edges {:fail    :validate-failure}}

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
            {:pute  (fn [_] [::puter/goto :fail :new-context])
             :edges {:fail    :validate-failure}}

            :validate-failure
            {:pute (fn [ctx] ctx)}}}
          {}))))

(deftest test-splice-node
  (is (= {:id   :user-signup
          :init :validate
          :nodes
          {:validate
           {:pute  identity
            :edges {:default :validate-success}}

           ;; this is spliced in in between :validate and :insert-user
           :validate-success
           {:pute  identity
            :edges {:default :insert-user}}

           :insert-user
           {:pute  identity
            :edges {:default :user-signup-default}}}}
         (puter/splice-node
          {:id   :user-signup
           :init :validate
           :nodes
           {:validate
            {:pute  identity
             :edges {:default :insert-user}}

            :insert-user
            {:pute  identity
             :edges {:default :user-signup-default}}}}
          {:node-name       :validate-success
           :node            {:pute identity}
           :input-node-name :validate}))))

(deftest test-schemas
  (testing "validates pute input"
    (is (thrown?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs cljs.core/ExceptionInfo)
         (puter/execute
          {:id   :user-signup
           :init :validate

           :nodes
           {:validate
            {:pute    (fn [ctx] ctx)
             :schemas {::puter/input [:map]}}}}
          :not-a-map)))

    (is (puter/execute
         {:id   :user-signup
          :init :validate

          :nodes
          {:validate
           {:pute    (fn [ctx] ctx)
            :schemas {::puter/input [:map]}}}}
         {:is :a-map})))

  (testing "validates edge output"
    (is (thrown?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs cljs.core/ExceptionInfo)
         (puter/execute
          {:id   :user-signup
           :init :validate

           :nodes
           {:validate
            {:pute    (fn [_] :not-a-map)
             :edges   {:default :insert-user}
             :schemas {:default [:map]}}

            :insert-user
            {:pute identity}}}
          nil))))

  (testing "validates output"
    (is (thrown?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs cljs.core/ExceptionInfo)
         (puter/execute
          {:id   :user-signup
           :init :validate

           :nodes
           {:validate
            {:pute    (fn [_] :not-a-map)
             :schemas {::puter/output [:map]}}}}
          nil)))))

(deftest validate-graph-:init
  (testing "throws when :init refers to invalid nodes"
    (is (thrown-with-msg?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs cljs.core/ExceptionInfo)
         #":init does not refer to valid node"
         (puter/execute
          {:id :test
           :init :nonexistent-node
           :nodes {}}
          {}))))

  (testing "throws when node edges refer to undefined nodes"
    (is (thrown-with-msg?
         #?(:clj clojure.lang.ExceptionInfo
            :cljs cljs.core/ExceptionInfo)
         #"node edges refer to undefined nodes"
         (puter/execute
          {:id :test
           :init :start
           :nodes
           {:start
            {:pute (fn [_])
             :edges {:default :undefined-node}}}}
          {})))))
