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
[Getting started](docs/src/main/mdoc/getting-started.md) ·
[Modules](docs/src/main/mdoc/modules.md) ·
[Conditionals](docs/src/main/mdoc/conditionals.md) ·
[API](docs/src/main/mdoc/api.md)

## Installation

The library is unreleased. Run `sbt publishLocal`, then use the version from
`sbt 'show allJVM/version'`:

```scala
libraryDependencies += "io.github.darkwingmcquack" %% "pspsps" % "VERSION"
```

Use `%%%` for Scala.js. Individual artifacts are `pspsps-core`, `pspsps-effect`,
`pspsps-fs2`, and `pspsps-laws`.

## Development

```sh
nix develop path:.
sbt ci
sbt 'docs/tlSitePreview'
```

Without Nix, install JDK 17 or 21, sbt, and Node.js 24. The site preview runs at
http://localhost:4242/ with API docs at `/api/`. Build static HTML with
`sbt 'docs/tlSite'`; run the example with `sbt 'examples/run'`.

Release tags publish artifacts. Pushes to `master` publish documentation to
`gh-pages`; set GitHub Pages to that branch's `/ (root)` directory.

[Apache-2.0](LICENSE).
