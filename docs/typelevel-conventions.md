# Typelevel build conventions

The pspsps build follows current upstream library sources, reviewed on
2026-10-14. The goal is a familiar contributor experience with a small runtime
API and independently consumable modules.

| Reference | Convention adopted |
| --- | --- |
| [Mouse build](https://github.com/typelevel/mouse/blob/main/build.sbt) | `tlCrossRootProject`, portable modules, sbt-typelevel versioning, `TypelevelSitePlugin` |
| [Kittens build](https://github.com/typelevel/kittens/blob/master/build.sbt) | MUnit and Discipline, explicit validation aliases, formatting and MiMa checks |
| [FS2 build](https://github.com/typelevel/fs2/blob/main/build.sbt) | Separate law/testing concerns, minimal runtime dependencies, mdoc-backed site, unified API documentation |
| [Cats Effect build](https://github.com/typelevel/cats-effect/blob/series/3.x/build.sbt) | Shared platform tests, law artifacts, deliberate CI matrix, lifecycle and cancellation testing |
| [Typelevel template](https://github.com/typelevel/typelevel.g8) | Project metadata, build/plugin conventions, contributor toolchain |
| [Typelevel Nix](https://github.com/typelevel/typelevel-nix) | Pinned development shell with Scala toolchain support |
| [sbt-typelevel site guide](https://typelevel.org/sbt-typelevel/site.html) | mdoc + Laika/Helium, `tlSite`, local preview, generated deployment workflow |

## Build and modules

The root aggregates Scala 2.13 and Scala 3 projects for JVM and Scala.js. Core, Cats Effect, and FS2
syntax have separate dependency graphs. The umbrella artifact depends on all
three; reusable laws live in a dedicated module, and application examples and
documentation remain unpublished build projects.

sbt-typelevel provides common compiler, license, formatting, compatibility,
versioning, and workflow conventions. GitHub Actions files are generated from
the sbt settings; contributors regenerate them after build changes.

## Nix development toolchain

The flake imports Typelevel Nix's overlay and `typelevelShell`, following the
same input graph as [FS2's flake](https://github.com/typelevel/fs2/blob/main/flake.nix).
The checked-in lock graph was taken from
[FS2's authoritative lockfile](https://github.com/typelevel/fs2/blob/main/flake.lock)
on 2026-10-14, pinning Typelevel Nix, nixpkgs, flake-utils, and their dependencies.
The graph has been evaluated for this repository; complete shell execution
results are recorded in [verification status](../VERIFICATION.md).

Supported hosts are `x86_64-linux`, `aarch64-linux`, and `aarch64-darwin`.
The default shell uses JDK 17; `nix develop path:.#jdk21` selects JDK 21.
Both include sbt, Scala CLI, Node.js 24 for Scala.js, Python 3, and Git.
Scala Native tooling is disabled because the library currently targets JVM and
Scala.js.

## Microsite

`TypelevelSitePlugin` combines checked mdoc examples with Laika's Helium theme.
This is the current sbt-typelevel site approach: it needs no Ruby/Jekyll toolchain.
Source guides live in `docs/src/main/mdoc`, with navigation ordering defined in
`directory.conf`. The standard theme provides responsive navigation, code
highlighting, and light/dark mode support. The site embeds unified Scaladoc
under `api/`, with a local API navigation link; it works before publication.

```sh
sbt 'docs/mdoc'
sbt 'docs/tlSite'
sbt 'docs/tlSitePreview'
```

`tlSite` renders the site; `tlSitePreview` serves it at `http://localhost:4242`.
The configured repository is `DarkWingMcQuack/pspsps` with namespace
`io.github.darkwingmcquack`. The generated workflow publishes the site on release
tags. Tooling choices do not imply Typelevel organizational membership.

## Tests and release discipline

MUnit and ScalaCheck check behavior; Discipline packages constructor coherence
as reusable rulesets. Compiler contracts protect overload inference alongside
MiMa's binary checks. Cats Effect tests coordinate through Deferred and
TestControl for deterministic lifecycle and stream checks.

Publishing needs a real owned Maven namespace, signing/account credentials, a
passing verification pipeline, and an intentional release tag. Follow the
[release guide](../RELEASING.md). The executed checks and remaining constraints
are recorded in [verification status](../VERIFICATION.md).
