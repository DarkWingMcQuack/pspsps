# Changelog

## Unreleased — 0.1 series

- Named the library **pspsps**, with repository `DarkWingMcQuack/pspsps` and
  Maven namespace `io.github.darkwingmcquack`.
- Cross-built Cats, Cats Effect, and FS2 syntax for Scala 2.13 and Scala 3,
  on JVM and Scala.js.
- Added a curated `common` import for everyday transformer, conditional,
  Resource, Ref, Stream, Chunk, and Java nullable syntax; `all` exposes the
  complete API with specialized families.
- Added `.nullable`, mapping Java null values to None and other values to Some.
- Added staged Either, Option, EitherT, and OptionT conditionals with by-name
  selection, both Either branch orders, and explicit pure/effectful methods.
- Split Resource interop and advanced acquisition into focused imports while
  preserving `resource` as the complete family.
- Kept the API focused by using Cats' existing inner-type lifting and Nested
  syntax, and FS2/Cats Effect constructors directly where additional names add
  little value. Staged factory namespaces expose their conditional API only.
- Added reusable constructor and conditional laws, Discipline rules,
  ScalaCheck properties, lifecycle tests, and compiler inference contracts.
- Added sbt-typelevel, a pinned Typelevel Nix flake, formatting, compatibility,
  generated GitHub Actions, and a JVM coverage workflow.
- Added a Laika/Helium microsite with checked mdoc guides, embedded unified
  Scaladoc, a generated source inventory, and a runnable example.

There is no published release yet. Actual execution results are recorded in
[VERIFICATION.md](VERIFICATION.md).
