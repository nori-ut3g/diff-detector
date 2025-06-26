(ns diff-detector.core
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]))

(defn namespace-pattern->regex
  "Converts a namespace pattern like 'app.security.*' to a regex pattern.
  Escapes dots and hyphens, converts * to .*, and anchors with ^ and $."
  [pattern]
  (-> pattern
      (str/replace "." "\\.")
      (str/replace "-" "\\-")
      (str/replace "*" ".*")
      (#(str "^" % "$"))
      re-pattern))

(defn file-path->namespace
  "Converts a file path to a Clojure namespace.
  Removes everything up to /src/, removes file extensions,
  converts / to . and _ to -."
  [file-path]
  (when (and file-path 
             (re-matches #".*\.(clj|cljs|cljc)$" file-path))
    (let [normalized (cond
                      ;; Handle paths with /src/
                      (str/includes? file-path "/src/")
                      (second (str/split file-path #"/src/" 2))
                      
                      ;; Handle paths starting with src/
                      (str/starts-with? file-path "src/")
                      (subs file-path 4)
                      
                      :else nil)]
      (when normalized
        (-> normalized
            (str/replace #"\.(clj|cljs|cljc)$" "")
            (str/replace "/" ".")
            (str/replace "_" "-"))))))

(defn matches-namespace?
  "Checks if a file path matches any of the namespace patterns."
  [file-path namespace-patterns]
  (if-let [ns (file-path->namespace file-path)]
    (boolean (some (fn [pattern]
                     (let [regex (namespace-pattern->regex pattern)]
                       (re-matches regex ns)))
                   namespace-patterns))
    false))

(defn get-changed-files
  "Executes git diff --name-only between two commits and returns the list of changed files."
  [base-commit target-commit]
  (let [result (shell/sh "git" "diff" "--name-only" base-commit target-commit)]
    (if (zero? (:exit result))
      (->> (:out result)
           str/trim
           str/split-lines
           (remove str/blank?))
      (throw (ex-info "Git command failed"
                      {:exit-code (:exit result)
                       :error (:err result)})))))

(defn detect-critical-changes
  "Main detection logic that finds changes in critical namespaces.
  Returns a map with :changed? boolean and :changed-files vector."
  [base-commit target-commit namespace-patterns]
  (try
    (let [changed-files (get-changed-files base-commit target-commit)
          clojure-files (filter #(re-matches #".*\.(clj|cljs|cljc)$" %) changed-files)
          critical-files (filter #(matches-namespace? % namespace-patterns) clojure-files)]
      {:changed? (boolean (seq critical-files))
       :changed-files (vec critical-files)})
    (catch Exception e
      (throw (ex-info "Failed to detect changes"
                      {:base-commit base-commit
                       :target-commit target-commit
                       :namespace-patterns namespace-patterns}
                      e)))))

(defn detect-function-changes
  "Placeholder for future function-level detection."
  [base-commit target-commit function-patterns]
  (throw (UnsupportedOperationException. "Function-level detection not yet implemented")))

(defn detect-metadata-changes
  "Placeholder for future metadata/annotation detection."
  [base-commit target-commit metadata-patterns]
  (throw (UnsupportedOperationException. "Metadata detection not yet implemented")))
