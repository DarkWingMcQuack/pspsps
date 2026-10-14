# Verification status

Verified locally on 2026-10-14. pspsps is **unreleased**, with compilation,
tests, documentation, and repository tooling executed successfully.

## Executed test matrix

| Scala | JVM, JDK 17 | Scala.js, Node 24 | JVM, JDK 21 |
| --- | --- | --- | --- |
| 2.13.18 | 151 passed | 151 passed | Not a CI target |
| 3.3.8 | 198 passed | 198 passed | 198 passed |

The shared suites exercise constructor coherence, conditional selection,
resource release on success/error/cancellation, failed acquisition, stream
output and timing, and reference-only `.nullable` conversion. Discipline
checks run over Option, List, and Either. Both compilers verify curated versus
exhaustive imports and explicit branch disambiguation. Compiler-specific
contracts additionally cover inference, rejected stage operations, and effect
subclasses; this accounts for the different suite totals.

## Commands and results

The sbt tasks were run with the pinned sbt 1.13.0 launcher. Caches and sbt's
runtime files were redirected to temporary writable directories for this
restricted workspace; those environment adjustments are not library settings.

| Command / check | Result |
| --- | --- |
| `sbt ci` | Passed workflow freshness, both Scala versions' headers/formatting, JVM/JS suites, example compilation, and site generation |
| `sbt 'examples/run'` | Passed; printed `hello` in a forked JVM |
| `sbt '+mimaReportBinaryIssues'` | Completed; no published compatibility baseline yet |
| `sbt '+doc'` | Passed API documentation for both Scala versions |
| `sbt 'docs/tlSite'` | Passed mdoc and Laika rendering, including embedded unified Scala 3 API documentation |
| `sbt coverageJVM` | Produced HTML/XML/Cobertura reports; aggregate statement coverage 93.06%, branch coverage 100% |
| `sbt evicted` | Inspected the resolved dependency graph; upstream documentation tooling reports transitive eviction warnings |
| `python3 scripts/check_repository.py` | Passed inventory, license, file, and source Markdown link checks |
| Pinned nixfmt 1.4.0 | Passed `flake.nix` formatting check |
| `nix flake check path:.` | Built and passed the Linux repository check |
| `nix flake check path:. --no-build --all-systems` | All configured Linux and Apple Silicon outputs evaluated |
| `nix develop path:.` | Started successfully with JDK 17.0.20, Node 24.19.0, Python 3.14.7 |

The standard CI matrix was tested with Node 24.21.0 and JDK 17.0.20; the JDK 21
check used 21.0.12.1. Scala.js checks also ran inside the pinned Nix shell.
The coverage aggregate includes the unpublished example project; its application
entry point is outside the test suites. Coverage is a maintenance signal, not
proof of every possible effect behavior.

Rendered HTML was checked separately: 10,609 local file links across 85 pages
resolved, including the local API tree and source links. mdoc checks Scala
snippets; Laika validates the assembled site's references. mdoc's independent
link hygiene is disabled because generated reference/API pages join the tree
at the Laika stage.

## Current source surface

The generated [API inventory](docs/reference.md) records 76 Scala 3 extension
declarations and 72 distinct method names, with matching Scala 2.13 syntax.
The staged builders are documented in the [conditional guide](docs/conditionals.md).
The six requested redundant helpers and unrelated companion factory forwarding
are removed. `common` is curated; `all` remains exhaustive.

## Release limits

No Maven Central artifacts, release tags, or GitHub Pages deployment have been
created. Namespace credentials, signing secrets, and Pages configuration must
be set before the first intentional release. MiMa cannot compare against a
previous published version yet. Local execution verifies workflow generation
and tasks; it is not a report of a hosted GitHub Actions run.

The bootstrap checks ran before the initial commit, so sbt reported
`0.1-SNAPSHOT` and an unborn `HEAD`. Subsequent versions are derived from Git
history. No remote publication was made during verification.
