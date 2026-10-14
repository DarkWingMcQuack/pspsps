# pspsps

**Keep your Scala pipelines flowing through Cats, Cats Effect, and FS2.**

pspsps is an opinionated syntax library for **Scala 2.13 and Scala 3**, on JVM
and Scala.js. Familiar constructors sit where the value already is, returning
normal Cats, Cats Effect, and FS2 types.

```scala
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

On Scala 2.13, write `import pspsps.syntax.common._`; the pipeline stays the same.

[Getting started](docs/src/main/mdoc/getting-started.md) ·
[Conditional builders](docs/conditionals.md) ·
[API reference](docs/reference.md) ·
[Design](docs/design.md) ·
[Contributing](CONTRIBUTING.md)

## Everyday fluent syntax

```scala
import cats.effect.IO
import pspsps.syntax.common.*

Option(42).asOptionT[IO]
IO.pure(42).asEitherTLiftF[String]
List(1, 2, 3).asStreamEmits[IO]
IO.pure(42).asResourceEval
0.asRef[IO]

true.asEitherCond.withLeft("missing").withRight(42)
IO.pure(true).asOptionTCond.withSome(IO.pure(42))

val javaValues = new java.util.HashMap[String, String]()
javaValues.get("missing").nullable // None
```

The recommended `common` import includes OptionT, EitherT, conditionals, basic
Resource construction, Ref, Stream, Chunk, and `.nullable` for Java interop.
`.nullable` accepts reference types, including boxed Java values; primitives are excluded.
Choose `all` for the complete API, including IorT, WriterT, Kleisli, StateT, Pull,
and advanced Resource construction, or add focused imports as needed.

`asResourceEval` registers no finalizer; use `acquire.asResourceMake(release)`
for a managed resource. `asEitherTLiftF[E]` preserves failures in the outer
effect; use `fa.attempt.asEitherT` to represent them as Left. See the
[semantics guide](docs/src/main/mdoc/semantics.md).

## Choose your dependencies

| Artifact | Aggregate import | Direct dependency |
| --- | --- | --- |
| `pspsps-core` | `pspsps.syntax.core.*` | Cats Core |
| `pspsps-effect` | `pspsps.syntax.effect.*` | Cats Effect Kernel |
| `pspsps-fs2` | `pspsps.syntax.streaming.*` | FS2 Core |
| `pspsps` | `pspsps.syntax.common.*` or `all.*` | All three syntax modules |
| `pspsps-laws` | `pspsps.laws.discipline` | Core and law-testing libraries |

The syntax modules are independent. Focused imports such as
`pspsps.syntax.optionT.*`, `pspsps.syntax.resource.*`, and
`pspsps.syntax.stream.*` keep each scope small. Use one aggregate or the focused
families you need. Replace `*` with `_` in Scala 2 imports.

The build cross-compiles Scala **2.13.18 and 3.3.8** for JVM and Scala.js.
Scala Native is outside the current support matrix. Pins and upstream sources
are recorded in [dependency provenance](docs/dependencies.md).

## Work on the library

The [Typelevel Nix flake](flake.nix) supplies the development toolchain:

```sh
nix develop path:.
sbt verify
sbt 'examples/run'
sbt 'docs/tlSitePreview'
```

The preview runs at `http://localhost:4242`. `sbt docs/tlSite` generates the
microsite and embedded unified Scaladoc under `docs/target/docs/site`.
Without Nix, install JDK 17 or 21, sbt, Node.js 24, and Python 3.
`verifyJVM` and `verifyJS` run each platform across both Scala versions;
`docs/mdoc` checks the Scala 3 documentation examples. `sbt coverageJVM`
generates the JVM coverage report.

The repository follows [Mouse](https://github.com/typelevel/mouse),
[Kittens](https://github.com/typelevel/kittens), and [FS2](https://github.com/typelevel/fs2):
sbt-typelevel, portable modules, MUnit and Discipline, checked mdoc guides, and
a Laika/Helium site. See the [build notes](docs/typelevel-conventions.md).

## Releases

The repository is [DarkWingMcQuack/pspsps](https://github.com/DarkWingMcQuack/pspsps),
with Maven namespace `io.github.darkwingmcquack`. It is **unreleased**;
[VERIFICATION.md](VERIFICATION.md) records actual checks and remaining limits.
After a release, use its actual version:

```scala
libraryDependencies += "io.github.darkwingmcquack" %% "pspsps" % "YOUR_RELEASE"
```

For Scala.js, use `%%%` with sbt-scalajs. For local use, run `sbt publishLocal`
and use the exact version from `sbt 'show allJVM/version'`.
[RELEASING.md](RELEASING.md) covers Maven Central and site deployment.

## License

[Apache-2.0](LICENSE). Contributions are welcome under the same license.
