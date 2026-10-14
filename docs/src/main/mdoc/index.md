# pspsps

**Fluent Scala. Familiar types.**

pspsps adds opinionated postfix constructors and staged conditionals to
Cats, Cats Effect, and FS2. Keep a left-to-right pipeline readable as it moves
through transformers, managed resources, and streams.

```scala mdoc
import cats.effect.IO
import pspsps.syntax.common.*
import fs2.Stream

val current: IO[Option[String]] = IO.pure(Some("hello"))
val save: String => IO[String] = value => IO.pure(value.trim)

val updated: Stream[IO, String] =
  current
    .asOptionT
    .filter(_.nonEmpty)
    .semiflatMap(save)
    .value
    .asStreamEval
    .unNone
```

The result is a normal `Stream[IO, String]`. Continue composing it with FS2;
pspsps delegates construction to the libraries you already use.

## Start here

- [Getting started](getting-started.md): build your first pipeline.
- [Modules and imports](modules.md): choose the dependencies you need.
- [Conditional builders](conditionals.md): express branch selection fluently.
- [API reference](reference.md): inspect every extension signature.
- [Design notes](design.md): understand the naming and dependency boundaries.
- [Evaluation and resource safety](semantics.md): understand the execution contract.
- [Laws and testing](laws.md): verify constructor coherence and effect behavior.
- [Contributing](contributing.md): develop the library and preview this site.

## A small syntax library with a serious foundation

The syntax modules target Scala 2.13 and Scala 3.3 on JVM and Scala.js. The build uses
sbt-typelevel, MUnit, ScalaCheck, Discipline, and Cats Effect testing tools.
This documentation is compiled with mdoc and rendered by Typelevel's
Laika/Helium site plugin.

The library is currently unreleased. Publishing coordinates and site deployment
are configured by the repository maintainer before the first release.
