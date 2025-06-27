# diff-detector

A Leiningen plugin to detect changes in critical namespaces between Git commits.

Perfect for regulated industries (healthcare, finance) where you need to monitor changes in security-critical code paths.

## Installation

Add to your `project.clj`:

```clojure
:plugins [[diff-detector "0.1.0"]]
```

## Configuration

Add configuration to your `project.clj`:

```clojure
:diff-detector {:namespaces ["my-app.security.*"
                             "my-app.payment.core"
                             "my-app.compliance.*"]}
```

## Usage

### Basic usage
```bash
lein diff-detector
```

### With detailed summary
```bash
lein diff-detector --summary
```

### Compare specific commits/branches
```bash
lein diff-detector --base main --target feature-branch
```

### Show help
```bash
lein diff-detector --help
```

## Environment Variables

- `DIFF_DETECTOR_BASE` - Default base commit (fallback: origin/main)
- `DIFF_DETECTOR_TARGET` - Default target commit (fallback: HEAD)

## Exit Codes

- `0` - No critical namespace changes detected
- `1` - Critical namespace changes detected (useful for CI/CD)

## Use Cases

- **CI/CD Integration**: Fail builds when critical code changes
- **Code Review**: Automatically flag security-sensitive changes
- **Compliance**: Track changes in regulated code paths
- **Risk Management**: Monitor high-impact code modifications

## Example Output

```
⚠️  Critical namespace changes detected!

Changed files in critical namespaces:
  - src/security/auth/core.clj
  - src/payment/gateway/stripe.clj

Summary:
  Total critical files changed: 2
```

## License

Copyright © 2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.