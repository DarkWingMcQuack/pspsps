## From an optional value to a stream

Combine Cats transformers and FS2 streams in one pipeline:

```scala mdoc:silent
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

The result is a normal `Stream[IO, String]`.

## Scala 2.13 and Scala 3, on JVM and Scala.js

Use `import pspsps.syntax.common.*` on Scala 3 or
`import pspsps.syntax.common._` on Scala 2.13. Choose focused imports for
individual syntax families, or `all` for the complete API.

The library is currently unreleased. See [getting started](getting-started.md)
for local installation and a complete example.
