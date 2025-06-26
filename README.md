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

Configure critical namespaces in your `project.clj`:

```clojure
:diff-detector {:namespaces ["my-app.security.*" 
                             "my-app.payment.core"
                             "my-app.compliance.*"]
                :base-commit "origin/main"
                :target-commit "HEAD"}
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

# Show help
lein diff-detector --help
```

### Command Line Options

- `-b, --base BASE` - Base commit to compare from (default: origin/main)
- `-t, --target TARGET` - Target commit to compare to (default: HEAD)
- `-s, --summary` - Show detailed summary
- `-h, --help` - Show help

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

When critical changes are detected:

```
⚠️  Critical namespace changes detected!

Changed files in critical namespaces:
  - src/my_app/security/auth.clj
  - src/my_app/payment/gateway.clj
```

When no critical changes are detected:

```
✅ No critical namespace changes detected.
```

## CI/CD Integration

The plugin is designed to work seamlessly with CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Check critical namespace changes
  run: lein diff-detector --base ${{ github.event.pull_request.base.sha }} --target ${{ github.sha }}
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