(ns leiningen.diff-detector
  (:require [clojure.tools.cli :as cli]
            [diff-detector.core :as core]
            [clojure.string :as str]))

(def cli-options
  [["-b" "--base BASE" "Base commit"
    :default "origin/main"]
   ["-t" "--target TARGET" "Target commit"
    :default "HEAD"]
   ["-s" "--summary" "Show detailed summary"]
   ["-h" "--help" "Show help"]])

(defn- format-output
  "Formats the output based on the results and options."
  [{:keys [changed? changed-files]} show-summary?]
  (if changed?
    (do
      (println "⚠️  Critical namespace changes detected!")
      (println)
      (println "Changed files in critical namespaces:")
      (doseq [file changed-files]
        (println (str "  - " file)))
      (when show-summary?
        (println)
        (println "Summary:")
        (println (str "  Total critical files changed: " (count changed-files))))
      1)  ; Exit code 1 for CI/CD
    (do
      (println "✅ No critical namespace changes detected.")
      0))) ; Exit code 0

(defn diff-detector
  "Leiningen plugin to detect changes in critical namespaces.
  
  Usage:
    lein diff-detector                                    # Basic usage
    lein diff-detector --summary                          # With summary
    lein diff-detector --base main --target feature-branch # Specific commits
    lein diff-detector --help                             # Show help
  
  Configuration in project.clj:
    :diff-detector {:namespaces [\"my-app.security.*\" \"my-app.payment.core\"]}
  
  Environment variables:
    DIFF_DETECTOR_BASE    - Default base commit (fallback: origin/main)
    DIFF_DETECTOR_TARGET  - Default target commit (fallback: HEAD)"
  [project & args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)
        config (get project :diff-detector {})
        base-commit (or (:base options) "origin/main")
        target-commit (or (:target options) "HEAD")
        namespaces (:namespaces config)]
    
    (cond
      errors
      (do
        (doseq [error errors]
          (println error))
        (System/exit 1))
      
      (:help options)
      (do
        (println "Diff Detector - Leiningen plugin to detect changes in critical namespaces")
        (println)
        (println "Usage: lein diff-detector [options]")
        (println)
        (println "Options:")
        (println summary)
        (println)
        (println "Configuration in project.clj:")
        (println "  :diff-detector {:namespaces [\"my-app.security.*\" \"my-app.payment.core\"]}")
        (println)
        (println "Environment variables:")
        (println "  DIFF_DETECTOR_BASE    - Default base commit (fallback: origin/main)")
        (println "  DIFF_DETECTOR_TARGET  - Default target commit (fallback: HEAD)")
        (System/exit 0))
      
      (empty? namespaces)
      (do
        (println "Error: No critical namespaces configured.")
        (println "Add :diff-detector {:namespaces [...]} to your project.clj")
        (System/exit 1))
      
      :else
      (try
        (let [result (core/detect-critical-changes base-commit target-commit namespaces)
              exit-code (format-output result (:summary options))]
          (System/exit exit-code))
        (catch Exception e
          (println "Error:" (.getMessage e))
          (when (instance? clojure.lang.ExceptionInfo e)
            (println "Details:" (pr-str (ex-data e))))
          (System/exit 1))))))