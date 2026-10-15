# Evaluation and resource safety

pspsps keeps the semantics of its upstream constructors. The choice of
constructor decides where values live, how failures appear, and who owns cleanup.

## Wrapping and lifting

Wrapping preserves the existing outer effect; lifting introduces a new one.
Nested shapes benefit from explicit names:

```scala mdoc
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.syntax.all.*
import pspsps.syntax.common.*
import fs2.{Pull, Stream}

val nested: Option[Option[Int]] = 42.some.some
val wrapped: OptionT[Option, Int] = nested.asOptionTWrap
val lifted: OptionT[IO, Option[Int]] = nested.toOptionT[IO]
```

An invariant `F[Some[A]]` may need widening to `F[Option[A]]` at construction.
The outer effect, domain error type, and successful value type are distinct.

## Effect failure and domain failure

`asEitherTLiftF` puts successful values in Right and retains failures in F.
Use the effect's own error capture when a Left is the intended policy:

```scala mdoc
val operation: IO[Int] = IO.raiseError(new RuntimeException("unavailable"))
val keepFailure: EitherT[IO, String, Int] = operation.asEitherTLiftF[String]
val captureFailure: EitherT[IO, Throwable, Int] = operation.attempt.asEitherT
```

Constructing these values runs no IO. Their failures and results are observed
when the application executes the outer effect.

## Resource ownership

An effect can evaluate to a value without acquiring a cleanup obligation:

```scala mdoc
val acquire: IO[String] = "connection".pure[IO]
val release: String => IO[Unit] = _ => IO.unit

val evaluated = acquire.asResourceEval
val managed = acquire.asResourceMake(release)
val scoped: Stream[IO, String] = managed.asStreamResource
```

`asResourceEval` registers no cleanup. Use `asResourceMake`, `asResourceMakeCase`, or `asResourceAutoCloseable` for owned
handles. Resource consumption and FS2 scope termination determine release timing.

The `...MakeFull` variants, available through `resourceAdvanced`, `resource`,
or `all`, preserve Cats Effect's Poll contract. Acquisition
canceled before returning its value must clean up any partial acquisition itself.

## Suspend effects at their source

Scala evaluates an ordinary receiver before calling its extension.
`openHandle().pure[IO]` has already called `openHandle`; a later constructor cannot
make that call lazy. Use `IO.delay(openHandle())` or `IO.blocking(openHandle())`
at the original effect boundary. Already-started Future values retain Future's
execution behavior.

## Stream output and Pull results

Make the desired output shape explicit:

```scala mdoc
import pspsps.syntax.pull.*

val oneList: Stream[IO, List[Int]] = List(1, 2, 3).asStreamEmit[IO]
val threeInts: Stream[IO, Int] = List(1, 2, 3).asStreamEmits[IO]
val caught: Stream[IO, Either[Throwable, Int]] = 42.pure[IO].asStreamAttemptEval
val fromChunk: Stream[IO, Int] = fs2.Chunk(1, 2, 3).pure[IO].asStreamEvalUnChunk
val terminalResult: Pull[IO, Nothing, Int] = 42.pure[IO].asPullEval
val emittedValue: Pull[IO, Int, Unit] = 42.asPullOutput1[IO]
```

`asStreamAttemptEval` captures an effect failure as an emitted Either.
`asStreamEvalUnChunk` evaluates an effectful Chunk and emits its elements.
These names retain FS2's distinct constructors and output contracts.

`asPullEval` produces a terminal result without emitting it. Collection
constructors are for finite collections; they promise no bounded-memory
traversal of arbitrary or infinite collections.

## Conditional evaluation

[Conditional builders](conditionals.md) keep branches by-name. A Boolean
condition is evaluated once at entry; its selected branch expression is
constructed at completion. An `F[Boolean]` condition selects through F.flatMap.
For IO, that sequencing occurs when IO runs. Effects stored as pure payloads
retain their own semantics, and unselected branches are never evaluated by the
builder.
