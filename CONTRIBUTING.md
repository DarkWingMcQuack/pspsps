# Contributing

Start with a before/after Scala expression that demonstrates the improvement.
A good extension makes an existing construction read naturally in a pipeline,
with an obvious receiver, result type, evaluation policy, and upstream equivalent.

## Development environment

Run `nix develop path:.` for the pinned Typelevel toolchain. Alternatively install
JDK 17 or 21, sbt, Node.js 24, and Python 3. Use a Git checkout: sbt-typelevel derives
versions from Git history.

```sh
sbt fmt
sbt verify
sbt 'docs/tlSite'
python3 scripts/check_repository.py
```

`verifyJVM` runs both Scala versions on JVM, compiles the runnable example, and
builds the site with unified Scaladoc; `verifyJS` runs both versions on Scala.js.
`docs/tlSite` checks mdoc and
renders the documentation with Laika. Use `docs/tlSitePreview` for a local
preview at `http://localhost:4242`.

Before opening a pull request, run the build's `prePR` preparation command and
check the resulting diff. Run `githubWorkflowGenerate` when changing the build's
workflow settings, and include its generated files.

## Adding syntax

Choose the existing core, effect, or FS2 module according to the upstream
constructor. Keep core's dependency boundary small; independent integrations
belong in their own modules. Implement matching Scala 2.13 and Scala 3 syntax delegating to a public upstream
constructor, with its weakest supported typeclass constraints.
The [design notes](docs/design.md) describe the naming and inference contracts.

Add a constructor-coherence property and meaningful regression checks for the
new behavior. Resource and stream changes need acquisition, release, error, and
cancellation coverage where applicable. Inference changes need positive and
negative compile contracts, including normal Cats syntax imports. Coordinate
concurrent tests with `Deferred` or Cats Effect `TestControl`, without real sleeps.

Document the operation in the mdoc guide. After changing the public API,
regenerate the source inventory and check it:

```sh
python3 scripts/api_inventory.py
python3 scripts/check_repository.py
```

The generated [reference](docs/reference.md) and JSON inventory describe receiver
shapes and signatures. They supplement Scaladoc and behavior tests.

## Compatibility and quality

The 0.1 series uses early-semver. Once an artifact baseline exists, review MiMa
results for public API changes. Binary checks supplement the compiler contracts;
changes to overload inference can affect consumers even when MiMa accepts them.

Keep errors and cleanup visible. Avoid unsafe runners, casts, global implicit
conversions, hidden retries, silent error suppression, and broad `Any` extension
names. New syntax should earn its place through a concrete use case.

Contributions are provided under [Apache-2.0](LICENSE). Keep discussion respectful
and specific. Report sensitive issues through the [security process](SECURITY.md).
