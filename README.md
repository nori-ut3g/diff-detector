# diff-detector

A Leiningen plugin to detect changes in critical namespaces between Git commits. Designed for regulated industries (healthcare, finance) where monitoring changes to critical code paths is essential for compliance and risk management.

## Features

- Detects changes in specified critical namespaces
- Supports exact namespace matching and wildcard patterns
- Integrates with CI/CD pipelines via exit codes
- Configurable through project.clj or command line options
- Provides detailed summaries of detected changes

## Installation

Add to your `~/.lein/profiles.clj`:

```clojure
{:user {:plugins [[diff-detector "0.1.0-SNAPSHOT"]]}}
```

Or add to your project's `project.clj`:

```clojure
:plugins [[diff-detector "0.1.0-SNAPSHOT"]]
```

## Configuration

### Project Configuration

Configure critical namespaces in your `project.clj`:

```clojure
:diff-detector {:namespaces ["my-app.security.*" 
                             "my-app.payment.core"
                             "my-app.compliance.*"]}
```

### Environment Variables

You can set default values for base and target commits using environment variables:

- `DIFF_DETECTOR_BASE` - Default base commit (fallback: origin/main)
- `DIFF_DETECTOR_TARGET` - Default target commit (fallback: HEAD)

```bash
# Example: Set defaults for your development environment
export DIFF_DETECTOR_BASE=develop
export DIFF_DETECTOR_TARGET=HEAD
```

### Namespace Patterns

- **Exact match**: `"my-app.payment.core"` - matches only this specific namespace
- **Wildcard match**: `"my-app.security.*"` - matches any namespace starting with `my-app.security.`

## Usage

### Basic Usage

```bash
# Check for changes between origin/main and HEAD
lein diff-detector

# With detailed summary
lein diff-detector --summary

# Specify custom commits
lein diff-detector --base main --target feature-branch

# Using branch patterns (wildcards)
lein diff-detector --base "origin/dev/*" --target HEAD

# Check all release branches against main
lein diff-detector --base origin/main --target "origin/release/*"

# Show help
lein diff-detector --help
```

### Command Line Options

- `-b, --base BASE` - Base commit/branch pattern to compare from (default: origin/main)
- `-t, --target TARGET` - Target commit/branch pattern to compare to (default: HEAD)
- `-s, --summary` - Show detailed summary
- `-h, --help` - Show help

### Branch Patterns

You can use wildcards (`*`) in branch names to check multiple branches at once:

- `origin/dev/*` - Matches all branches starting with `origin/dev/`
- `release/*` - Matches all branches starting with `release/`
- `*/1.2.0` - Matches all branches ending with `/1.2.0`

When using branch patterns, the tool will check all combinations of matching branches.

### Exit Codes

- **0**: No critical changes detected
- **1**: Critical changes detected (useful for CI/CD integration)

## Examples

### Healthcare Industry

```clojure
:diff-detector {:namespaces ["healthcare.patient.*"
                             "healthcare.billing.insurance"
                             "healthcare.compliance.hipaa.*"]}
```

### Financial Services

```clojure
:diff-detector {:namespaces ["fintech.transactions.*"
                             "fintech.compliance.*"
                             "fintech.risk.assessment"
                             "fintech.security.encryption"]}
```

### Output Examples

When using specific branches:
```
⚠️  Critical namespace changes detected!

Changed files in critical namespaces:
  - src/my_app/security/auth.clj
  - src/my_app/payment/gateway.clj
```

When using branch patterns:
```
Checking: origin/main -> origin/dev/1.1.0
  ⚠️  Changes detected:
    - src/my_app/security/auth.clj

Checking: origin/main -> origin/dev/1.2.0
  ⚠️  Changes detected:
    - src/my_app/security/auth.clj
    - src/my_app/payment/gateway.clj

⚠️  Critical namespace changes detected in one or more branches!

Summary:
  origin/main -> origin/dev/1.1.0: 1 files changed
  origin/main -> origin/dev/1.2.0: 2 files changed
```

When no critical changes are detected:
```
✅ No critical namespace changes detected in any branch combinations.
```

## CI/CD Integration

The plugin is designed to work seamlessly with CI/CD pipelines. Since base and target branches are dynamic in CI environments, always specify them via CLI options or environment variables rather than in project.clj.

### GitHub Actions

```yaml
# For pull requests
- name: Check critical namespace changes
  run: lein diff-detector --base origin/${{ github.base_ref }} --target origin/${{ github.head_ref }}
  continue-on-error: true

# Using environment variables
- name: Check critical namespace changes
  env:
    DIFF_DETECTOR_BASE: origin/${{ github.base_ref }}
    DIFF_DETECTOR_TARGET: origin/${{ github.head_ref }}
  run: lein diff-detector
  continue-on-error: true
  
- name: Require additional approval for critical changes
  if: failure()
  uses: actions/github-script@v6
  with:
    script: |
      github.rest.pulls.requestReviewers({
        owner: context.repo.owner,
        repo: context.repo.repo,
        pull_number: context.issue.number,
        reviewers: ['security-team', 'compliance-team']
      })
```

### GitLab CI

```yaml
check-critical-namespaces:
  script:
    - lein diff-detector --base origin/$CI_MERGE_REQUEST_TARGET_BRANCH_NAME --target origin/$CI_MERGE_REQUEST_SOURCE_BRANCH_NAME
  only:
    - merge_requests
  allow_failure: false
```

### Jenkins

```groovy
stage('Check Critical Namespaces') {
    steps {
        sh 'lein diff-detector --base origin/${ghprbTargetBranch} --target origin/${ghprbSourceBranch}'
    }
}
```

### CircleCI

```yaml
- run:
    name: Check critical namespace changes
    command: |
      if [ -n "$CIRCLE_PULL_REQUEST" ]; then
        lein diff-detector --base origin/$CIRCLE_PR_BASE_BRANCH --target origin/$CIRCLE_PR_HEAD_BRANCH
      fi
```

## Development

### Running Tests

```bash
lein test
```

### Local Installation

```bash
lein install
```

## Future Features

The plugin includes placeholder functions for future enhancements:

- **Function-level detection**: Track changes to specific functions
- **Metadata detection**: Monitor changes to metadata and annotations

## License

Copyright © 2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.