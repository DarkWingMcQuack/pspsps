# Dependency provenance

Versions were selected from upstream project sources and documentation viewed
on 2026-10-14. They are pinned for review, not described as an automatically
validated or universally latest combination. See
[verification status](../VERIFICATION.md) for dependency-resolution and execution
results.

| Component | Pin | Primary reference |
| --- | --- | --- |
| Scala | 2.13.18 / 3.3.8 | [Typelevel template defaults](https://github.com/typelevel/typelevel.g8/blob/main/src/main/g8/default.properties) |
| Cats | 2.13.0 | [release](https://github.com/typelevel/cats/releases/tag/v2.13.0) |
| Cats Effect | 3.7.1 | [release](https://github.com/typelevel/cats-effect/releases/tag/v3.7.1) |
| FS2 | 3.12.2 | [release](https://github.com/typelevel/fs2/releases/tag/v3.12.2) |
| sbt-typelevel | 0.8.7 | [official guide](https://typelevel.org/sbt-typelevel/) |
| sbt-typelevel-site | 0.8.7 | [site guide](https://typelevel.org/sbt-typelevel/site.html) |
| sbt | 1.13.0 | [template build properties](https://github.com/typelevel/typelevel.g8/blob/main/src/main/g8/project/build.properties) |
| Scala.js | 1.22.0 | [template plugins](https://github.com/typelevel/typelevel.g8/blob/main/src/main/g8/project/plugins.sbt) |
| sbt-scala-native | 0.5.12 | [sbt-typelevel FAQ](https://typelevel.org/sbt-typelevel/faq.html) (plugin pin for cross-project support; no Native artifacts) |
| sbt-scoverage | 2.4.4 | [plugin releases](https://github.com/scoverage/sbt-scoverage/releases) |
| mdoc | 2.9.0 | [template plugins](https://github.com/typelevel/typelevel.g8/blob/main/src/main/g8/project/plugins.sbt) |
| MUnit | 1.3.6 | [template build](https://github.com/typelevel/typelevel.g8/blob/main/src/main/g8/build.sbt) |
| munit-cats-effect | 2.2.0 | [official guide](https://typelevel.org/munit-cats-effect/) |
| Discipline MUnit | 2.0.0 | [Cats release history](https://github.com/typelevel/cats/releases) |
| Discipline Core | 1.7.0 | [releases](https://github.com/typelevel/discipline/releases) |
| munit-scalacheck | 1.3.1 | [Discipline MUnit build](https://github.com/typelevel/discipline-munit/blob/main/build.sbt) |

The pins live in `build.sbt` and `project/plugins.sbt`. Inspect `sbt evicted`
when changing them, and run both platform verification suites. This table records
provenance; [verification status](../VERIFICATION.md) records actual checks.

## Constructor references

The code delegates to these public APIs. Their exact signatures, rather than
memory of earlier versions, informed constraints and return types.

- [Cats OptionT](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/OptionT.scala)
- [Cats EitherT](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/EitherT.scala)
- [Cats IorT](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/IorT.scala)
- [Cats WriterT](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/WriterT.scala)
- [Cats Kleisli](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/Kleisli.scala)
- [Cats IndexedStateT / StateT](https://github.com/typelevel/cats/blob/v2.13.0/core/src/main/scala/cats/data/IndexedStateT.scala)
- [Cats Effect Resource](https://github.com/typelevel/cats-effect/blob/v3.7.1/kernel/shared/src/main/scala/cats/effect/kernel/Resource.scala)
- [Cats Effect Ref](https://github.com/typelevel/cats-effect/blob/v3.7.1/kernel/shared/src/main/scala/cats/effect/kernel/Ref.scala)
- [FS2 Stream](https://github.com/typelevel/fs2/blob/v3.12.2/core/shared/src/main/scala/fs2/Stream.scala)
- [FS2 Pull](https://github.com/typelevel/fs2/blob/v3.12.2/core/shared/src/main/scala/fs2/Pull.scala)


## Conditional constructor references

The new staged APIs are local implementations, not claims that these exact
signatures exist upstream. Pure completions delegate to the standard methods.

- [Scala Either.cond source](https://raw.githubusercontent.com/scala/scala/v2.13.18/src/library/scala/util/Either.scala): Boolean selection, by-name branches.
- [Scala Option source](https://raw.githubusercontent.com/scala/scala/v2.13.18/src/library/scala/Option.scala): Option.when and Some semantics.
- [Cats EitherT source](https://raw.githubusercontent.com/typelevel/cats/v2.13.0/core/src/main/scala/cats/data/EitherT.scala): native representation and existing three-argument conditional factory.
- [Scala extension methods](https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html): extension definitions and scope.

The Scala library source above is a reference for constructor behavior, not a
claim to have resolved the standard-library version selected transitively by
the cross-built Scala versions. The resolved dependency graph is verified by the build; recorded execution
results live in [verification status](../VERIFICATION.md).
