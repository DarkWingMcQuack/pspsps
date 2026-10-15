# pspsps

Postfix constructors for Cats, Cats Effect, and FS2 on Scala 2.13 and Scala 3,
for JVM and Scala.js.

```scala
import cats.effect.IO
import pspsps.syntax.common.*

val current: IO[Option[String]] = IO.pure(Some("hello"))

val stream = current
  .asOptionT
  .filter(_.nonEmpty)
  .value
  .asStreamEval
  .unNone
```

Use `common._` on Scala 2.13. `common` covers everyday constructors; `all`
includes every syntax family.

[Documentation](https://darkwingmcquack.github.io/pspsps/) ·
[Getting started](https://darkwingmcquack.github.io/pspsps/#/getting-started) ·
[API](https://darkwingmcquack.github.io/pspsps/api/)

## Installation

The library is unreleased. Run `sbt publishLocal`, then use the version from
`sbt 'show allJVM/version'`:

```scala
libraryDependencies += "io.github.darkwingmcquack" %% "pspsps" % "VERSION"
```

Use `%%%` for Scala.js. Individual artifacts are `pspsps-core`, `pspsps-effect`,
`pspsps-fs2`, and `pspsps-laws`.

[Apache-2.0](LICENSE).
