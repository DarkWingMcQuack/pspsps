# Evaluation and resource safety

pspsps keeps the semantics of its upstream constructors. The choice of
constructor decides where values live, how failures appear, and who owns cleanup.

## Wrapping and lifting

Wrapping preserves the existing outer effect; lifting introduces a new one.
Nested shapes benefit from explicit names:

```scala
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.syntax.all.*
import pspsps.syntax.common.*
import fs2.{Pull, Stream}

val nested: Option[Option[Int]] = 42.some.some
// nested: Option[Option[Int]] = Some(Some(42))
val wrapped: OptionT[Option, Int] = nested.asOptionTWrap
// wrapped: OptionT[[A >: Nothing <: Any] => Option[A], Int] = OptionT(
//   Some(Some(42))
// )
val lifted: OptionT[IO, Option[Int]] = nested.toOptionT[IO]
// lifted: OptionT[[A >: Nothing <: Any] => IO[A], Option[Int]] = OptionT(
//   Pure(Some(Some(42)))
// )
```

An invariant `F[Some[A]]` may need widening to `F[Option[A]]` at construction.
The outer effect, domain error type, and successful value type are distinct.

## Effect failure and domain failure

`asEitherTLiftF` puts successful values in Right and retains failures in F.
Use the effect's own error capture when a Left is the intended policy:

```scala
val operation: IO[Int] = IO.raiseError(new RuntimeException("unavailable"))
// operation: IO[Int] = Error(java.lang.RuntimeException: unavailable)
val keepFailure: EitherT[IO, String, Int] = operation.asEitherTLiftF[String]
// keepFailure: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Error(java.lang.RuntimeException: unavailable),
//     f = cats.data.EitherT$RightPartiallyApplied$$$Lambda$12706/0x00007fce6df94208@2931c529,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
val captureFailure: EitherT[IO, Throwable, Int] = operation.attempt.asEitherT
// captureFailure: EitherT[[A >: Nothing <: Any] => IO[A], Throwable, Int] = EitherT(
//   Attempt(Error(java.lang.RuntimeException: unavailable))
// )
```

Constructing these values runs no IO. Their failures and results are observed
when the application executes the outer effect.

## Resource ownership

An effect can evaluate to a value without acquiring a cleanup obligation:

```scala
val acquire: IO[String] = "connection".pure[IO]
// acquire: IO[String] = Pure("connection")
val release: String => IO[Unit] = _ => IO.unit
// release: Function1[String, IO[Unit]] = repl.MdocSession$MdocApp$$Lambda$12928/0x00007fce6e012440@7fff8611

val evaluated = acquire.asResourceEval
// evaluated: Resource[IO, String] = Eval(Pure("connection"))
val managed = acquire.asResourceMake(release)
// managed: Resource[IO, String] = Allocate(
//   cats.effect.kernel.Resource$$$Lambda$12714/0x00007fce6df9da40@1e2ad08f
// )
val scoped: Stream[IO, String] = managed.asStreamResource
// scoped: Stream[[A >: Nothing <: Any] => IO[A], String] = Stream(..)
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

```scala
import pspsps.syntax.pull.*

val oneList: Stream[IO, List[Int]] = List(1, 2, 3).asStreamEmit[IO]
// oneList: Stream[[A >: Nothing <: Any] => IO[A], List[Int]] = Stream(..)
val threeInts: Stream[IO, Int] = List(1, 2, 3).asStreamEmits[IO]
// threeInts: Stream[[A >: Nothing <: Any] => IO[A], Int] = Stream(..)
val caught: Stream[IO, Either[Throwable, Int]] = 42.pure[IO].asStreamAttemptEval
// caught: Stream[[A >: Nothing <: Any] => IO[A], Either[Throwable, Int]] = Stream(..)
val fromChunk: Stream[IO, Int] = fs2.Chunk(1, 2, 3).pure[IO].asStreamEvalUnChunk
// fromChunk: Stream[[A >: Nothing <: Any] => IO[A], Int] = Stream(..)
val terminalResult: Pull[IO, Nothing, Int] = 42.pure[IO].asPullEval
// terminalResult: Pull[[A >: Nothing <: Any] => IO[A], Nothing, Int] = Eval(
//   Pure(42)
// )
val emittedValue: Pull[IO, Int, Unit] = 42.asPullOutput1[IO]
// emittedValue: Pull[[A >: Nothing <: Any] => IO[A], Int, Unit] = Output(
//   Chunk(42)
// )
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
