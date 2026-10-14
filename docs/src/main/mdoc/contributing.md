# Contributing

Propose a concrete before/after expression. New syntax should make an existing
constructor clearer in a fluent pipeline while preserving its types,
constraints, and evaluation policy.

## Work locally

Use the repository's Typelevel Nix flake to get the development toolchain:

```sh
nix develop path:.
sbt fmt
sbt verify
sbt 'docs/tlSite'
python3 scripts/check_repository.py
```

Without Nix, install JDK 17 or 21, sbt, Node.js 24, and Python 3. Work from a Git
checkout so sbt-typelevel can derive the version.

## Preview the microsite

```sh
sbt 'docs/tlSitePreview'
```

Open `http://localhost:4242`. Source guides live in `docs/src/main/mdoc`; the
site plugin combines mdoc checking with Laika/Helium rendering and live preview.
`docs/mdoc` checks Scala 3 snippets alone, and `docs/tlSite` creates the static
site with embedded unified Scaladoc under `docs/target/docs/site`.

## Complete a change

Update the relevant guide and add behavior-focused regression coverage. Keep
core, effect, and FS2 dependency boundaries intact. Use constructor-coherence
laws for pure representation checks and lifecycle tests for observable effects.
Implement public syntax on both Scala versions. Changes to overload inference
need compiler contracts as well as MiMa checks.

Regenerate the API inventory after public syntax changes:

```sh
python3 scripts/api_inventory.py
python3 scripts/check_repository.py
```

Run `sbt prePR` before opening a pull request and inspect its diff. Workflow
settings belong in the build; run `sbt githubWorkflowGenerate` when changing
those settings and include the generated workflow files in the change.

The repository's contributor and release guides contain the full maintenance
process. Participation should be respectful, specific, and welcoming; sensitive
security reports belong in a private channel to the maintainer.
