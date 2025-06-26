(ns diff-detector.core-test
  (:require [clojure.test :refer :all]
            [diff-detector.core :as core]))

(deftest test-namespace-pattern->regex
  (testing "exact namespace pattern"
    (let [pattern "my-app.security.core"
          regex (core/namespace-pattern->regex pattern)]
      (is (re-matches regex "my-app.security.core"))
      (is (not (re-matches regex "my-app.security.core.extra")))
      (is (not (re-matches regex "prefix.my-app.security.core")))))
  
  (testing "wildcard namespace pattern"
    (let [pattern "my-app.security.*"
          regex (core/namespace-pattern->regex pattern)]
      (is (re-matches regex "my-app.security.auth"))
      (is (re-matches regex "my-app.security.encryption"))
      (is (re-matches regex "my-app.security.auth.oauth"))
      (is (not (re-matches regex "my-app.payments")))
      (is (not (re-matches regex "my-app.securityx")))))
  
  (testing "special characters in pattern"
    (let [pattern "my-app.payment-gateway.*"
          regex (core/namespace-pattern->regex pattern)]
      (is (re-matches regex "my-app.payment-gateway.stripe"))
      (is (re-matches regex "my-app.payment-gateway.paypal.api")))))

(deftest test-file-path->namespace
  (testing "standard file path conversion"
    (is (= "my-app.security.core" 
           (core/file-path->namespace "src/my_app/security/core.clj")))
    (is (= "my-app.security.core" 
           (core/file-path->namespace "test/src/my_app/security/core.clj")))
    (is (= "my-app.payment-gateway.api" 
           (core/file-path->namespace "src/my_app/payment_gateway/api.cljs")))
    (is (= "healthcare.patient.records" 
           (core/file-path->namespace "src/healthcare/patient/records.cljc"))))
  
  (testing "nested src directories"
    (is (= "my-app.core" 
           (core/file-path->namespace "project/src/my_app/core.clj")))
    (is (= "test.utils" 
           (core/file-path->namespace "foo/bar/src/test/utils.clj"))))
  
  (testing "files without src directory"
    (is (nil? (core/file-path->namespace "my_app/core.clj")))
    (is (nil? (core/file-path->namespace "build.clj")))))

(deftest test-matches-namespace?
  (testing "single exact pattern match"
    (let [patterns ["my-app.security.core"]]
      (is (core/matches-namespace? "src/my_app/security/core.clj" patterns))
      (is (not (core/matches-namespace? "src/my_app/security/auth.clj" patterns)))))
  
  (testing "single wildcard pattern match"
    (let [patterns ["my-app.security.*"]]
      (is (core/matches-namespace? "src/my_app/security/core.clj" patterns))
      (is (core/matches-namespace? "src/my_app/security/auth.clj" patterns))
      (is (core/matches-namespace? "src/my_app/security/encryption/aes.clj" patterns))
      (is (not (core/matches-namespace? "src/my_app/payments/core.clj" patterns)))))
  
  (testing "multiple patterns"
    (let [patterns ["my-app.security.*" "my-app.payment.core"]]
      (is (core/matches-namespace? "src/my_app/security/auth.clj" patterns))
      (is (core/matches-namespace? "src/my_app/payment/core.clj" patterns))
      (is (not (core/matches-namespace? "src/my_app/payment/gateway.clj" patterns)))
      (is (not (core/matches-namespace? "src/my_app/ui/dashboard.clj" patterns)))))
  
  (testing "non-clojure files"
    (let [patterns ["my-app.security.*"]]
      (is (not (core/matches-namespace? "src/my_app/security/README.md" patterns)))
      (is (not (core/matches-namespace? "src/my_app/security/auth.java" patterns))))))

(deftest test-get-changed-files
  (testing "mock git output parsing"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 0 
                     :out "src/my_app/security/auth.clj\nsrc/my_app/ui/views.clj\n\n"
                     :err ""})]
      (let [files (core/get-changed-files "main" "feature-branch")]
        (is (= ["src/my_app/security/auth.clj" "src/my_app/ui/views.clj"] files)))))
  
  (testing "git command failure"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 1 
                     :out ""
                     :err "fatal: bad revision 'invalid-ref'"})]
      (is (thrown? clojure.lang.ExceptionInfo
                   (core/get-changed-files "invalid-ref" "HEAD"))))))

(deftest test-detect-critical-changes
  (testing "detection with critical changes"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 0 
                     :out (str "src/my_app/security/auth.clj\n"
                              "src/my_app/security/encryption.clj\n"
                              "src/my_app/ui/dashboard.clj\n"
                              "README.md\n")
                     :err ""})]
      (let [result (core/detect-critical-changes 
                    "main" "feature" ["my-app.security.*"])]
        (is (:changed? result))
        (is (= ["src/my_app/security/auth.clj" 
                "src/my_app/security/encryption.clj"]
               (:changed-files result))))))
  
  (testing "detection without critical changes"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 0 
                     :out "src/my_app/ui/dashboard.clj\nsrc/my_app/utils/helpers.clj\n"
                     :err ""})]
      (let [result (core/detect-critical-changes 
                    "main" "feature" ["my-app.security.*" "my-app.payment.*"])]
        (is (not (:changed? result)))
        (is (empty? (:changed-files result))))))
  
  (testing "healthcare industry example"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 0 
                     :out (str "src/healthcare/patient/records.clj\n"
                              "src/healthcare/billing/insurance.clj\n"
                              "src/healthcare/ui/dashboard.cljs\n")
                     :err ""})]
      (let [result (core/detect-critical-changes 
                    "main" "feature" 
                    ["healthcare.patient.*" "healthcare.billing.payments"])]
        (is (:changed? result))
        (is (= ["src/healthcare/patient/records.clj"]
               (:changed-files result))))))
  
  (testing "finance industry example"
    (with-redefs [clojure.java.shell/sh 
                  (fn [& _] 
                    {:exit 0 
                     :out (str "src/fintech/transactions/processor.clj\n"
                              "src/fintech/compliance/aml.clj\n"
                              "src/fintech/api/public.clj\n")
                     :err ""})]
      (let [result (core/detect-critical-changes 
                    "origin/main" "HEAD" 
                    ["fintech.transactions.*" "fintech.compliance.*"])]
        (is (:changed? result))
        (is (= 2 (count (:changed-files result))))
        (is (contains? (set (:changed-files result)) 
                       "src/fintech/transactions/processor.clj"))
        (is (contains? (set (:changed-files result)) 
                       "src/fintech/compliance/aml.clj"))))))

(deftest test-false-positive-prevention
  (testing "similar but non-matching namespaces"
    (let [patterns ["my-app.security.core"]]
      (is (not (core/matches-namespace? "src/my_app/security/core_utils.clj" patterns)))
      (is (not (core/matches-namespace? "src/my_app/security_core.clj" patterns)))
      (is (not (core/matches-namespace? "src/my_app_security/core.clj" patterns)))))
  
  (testing "partial matches should not trigger"
    (let [patterns ["app.payments"]]
      (is (not (core/matches-namespace? "src/webapp/payments.clj" patterns)))
      (is (not (core/matches-namespace? "src/app/payments_v2.clj" patterns))))))

(deftest test-edge-cases
  (testing "empty patterns"
    (is (not (core/matches-namespace? "src/any/file.clj" []))))
  
  (testing "malformed file paths"
    (is (nil? (core/file-path->namespace "")))
    (is (nil? (core/file-path->namespace "   ")))
    (is (nil? (core/file-path->namespace nil))))
  
  (testing "unusual but valid namespace patterns"
    (let [pattern "a.b.c-d.e-f.*"
          regex (core/namespace-pattern->regex pattern)]
      (is (re-matches regex "a.b.c-d.e-f.g"))
      (is (re-matches regex "a.b.c-d.e-f.g.h")))))