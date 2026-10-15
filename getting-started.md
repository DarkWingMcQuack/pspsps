# Getting started

Postfix constructors for Cats, Cats Effect, and FS2 in Scala 2.13 and Scala 3.
Scala 2.13 uses `_` in place of `*` for wildcard imports.

## One import

```scala
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.instances.list.*
import cats.syntax.option.*
import pspsps.syntax.common.*
import fs2.Stream

val input: IO[Option[Int]] = IO.pure(Some(42))
// input: IO[Option[Int]] = Pure(Some(42))
val wrapped: OptionT[IO, Int] = input.asOptionT
// wrapped: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Pure(Some(42))
// )
val lifted: OptionT[IO, Int] = Option(42).asOptionT[IO]
// lifted: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Pure(Some(42))
// )
val plainEither: Either[String, Int] = Right(42)
// plainEither: Either[String, Int] = Right(42)
val either: EitherT[IO, String, Int] = plainEither.asEitherT[IO]
// either: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Pure(Right(42))
// )
val evaluated: Stream[IO, Int] = IO.pure(42).asStreamEval
// evaluated: Stream[[A >: Nothing <: Any] => IO[A], Int] = Stream(..)
```

## A complete pipeline

The IO actions execute when the stream runs.

```scala
final case class Content(value: String)

def update(
    key: String,
    content: Content,
    current: (String, Content) => IO[Option[Content]],
    save: Content => IO[Content]
): Stream[IO, Content] =
  current(key, content)
    .asOptionT
    .filter(_.value.nonEmpty)
    .semiflatMap(save)
    .value
    .asStreamEval
    .unNone

val updates = update(
  "key",
  Content("hello"),
  (_, content) => IO.pure(Some(content)),
  content => IO.pure(content)
)
// updates: Stream[[A >: Nothing <: Any] => IO[A], Content] = Stream(..)
```

## Wrapping, lifting, and pure values are different

```scala
val nested: Option[Option[Int]] = Some(Some(42))
// nested: Option[Option[Int]] = Some(Some(42))
val wrapNested: OptionT[Option, Int] = nested.asOptionTWrap
// wrapNested: OptionT[[A >: Nothing <: Any] => Option[A], Int] = OptionT(
//   Some(Some(42))
// )
val liftNested: OptionT[IO, Option[Int]] = nested.toOptionT[IO]
// liftNested: OptionT[[A >: Nothing <: Any] => IO[A], Option[Int]] = OptionT(
//   Pure(Some(Some(42)))
// )
val liftEffect: EitherT[IO, String, Int] = IO.pure(42).asEitherTLiftF[String]
// liftEffect: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = cats.data.EitherT$RightPartiallyApplied$$$Lambda$12743/0x00007f6e45fc76e8@511e5e48,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
val pureValue: EitherT[IO, String, Int] = 42.asEitherTPure[IO, String]
// pureValue: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Pure(Right(42))
// )

val captureErrors: EitherT[IO, Throwable, Int] =
  IO.raiseError[Int](new RuntimeException("example"))
    .attempt
    .asEitherT
// captureErrors: EitherT[[A >: Nothing <: Any] => IO[A], Throwable, Int] = EitherT(
//   Attempt(Error(java.lang.RuntimeException: example))
// )
```

`asEitherTLiftF` sends successful effect results to `Right`. It does not catch
errors of the outer effect. `attempt.asEitherT` makes that policy explicit.

## Collection output versus singleton output

```scala
val oneCollection: Stream[IO, List[Int]] = List(1, 2, 3).asStreamEmit[IO]
// oneCollection: Stream[[A >: Nothing <: Any] => IO[A], List[Int]] = Stream(..)
val threeElements: Stream[IO, Int] = List(1, 2, 3).asStreamEmits[IO]
// threeElements: Stream[[A >: Nothing <: Any] => IO[A], Int] = Stream(..)
val evaluatedElements: Stream[IO, Int] = IO.pure(List(1, 2, 3)).asStreamEvals
// evaluatedElements: Stream[[A >: Nothing <: Any] => IO[A], Int] = Stream(..)
val noElements: Stream[IO, Nothing] = IO.unit.asStreamExec
// noElements: Stream[[A >: Nothing <: Any] => IO[A], Nothing] = Stream(..)
```

`asStreamEmits` and `asStreamEvals` are for finite collections.
`asStreamEvals` follows FS2's conversion of the Foldable to a List.
Neither method promises streaming traversal of an infinite collection.

## Resource ownership

```scala
val acquire: IO[String] = IO.pure("connection")
// acquire: IO[String] = Pure("connection")
val release: String => IO[Unit] = _ => IO.unit
// release: Function1[String, IO[Unit]] = repl.MdocSession$MdocApp$$Lambda$12748/0x00007f6e45fc2940@e488426
val scoped = acquire.asResourceMake(release).asStreamResource
// scoped: Stream[IO, String] = Stream(..)
val unowned = acquire.asResourceEval
// unowned: Resource[IO, String] = Eval(Pure("connection"))
```

`asResourceEval` evaluates acquisition without registering cleanup.
`asResourceMake` pairs acquisition with a release action.

Do not put an already-acquired handle into `asResourcePure` expecting it to be
closed. Use `IO.blocking(openHandle()).asResourceAutoCloseable` for an
AutoCloseable acquired by blocking code.

## Constructors do not repair an eager receiver

Scala evaluates an ordinary receiver expression before calling its extension.
`IO.pure(openHandle())` has already opened the handle; later syntax cannot undo
that. Suspend with `IO.delay` or `IO.blocking` at the original effect boundary.
Already-running effects such as Future do not become lazy through this library.

## Focused syntax for other Cats types

The curated `common` import keeps these specialized families opt-in:

```scala
import pspsps.syntax.kleisli.*
import pspsps.syntax.writerT.*
import pspsps.syntax.stateT.*
val readLength: String => IO[Int] = key => IO.pure(key.length)
// readLength: Function1[String, IO[Int]] = repl.MdocSession$MdocApp$$Lambda$12756/0x00007f6e45fc3370@1605205e
val lookup = readLength.asKleisli
// lookup: Kleisli[IO, String, Int] = Kleisli(
//   repl.MdocSession$MdocApp$$Lambda$12756/0x00007f6e45fc3370@1605205e
// )
val logAndResult = (List("read"), 42).asWriterTFromTuple[IO]
// logAndResult: WriterT[[A >: Nothing <: Any] => IO[A], List[String], Int] = WriterT(
//   Pure((List("read"), 42))
// )
val stateTransition: Int => IO[(Int, String)] = n => IO.pure((n + 1, n.toString))
// stateTransition: Function1[Int, IO[Tuple2[Int, String]]] = repl.MdocSession$MdocApp$$Lambda$12757/0x00007f6e46004000@640c359
val state = stateTransition.asStateT
// state: IndexedStateT[IO, Int, Int, String] = cats.data.IndexedStateT@1d7fda6a
val initialRef = 0.asRef[IO]
// initialRef: IO[Ref[[A >: Nothing <: Any] => IO[A], Int]] = Delay(
//   thunk = cats.effect.IO$$$Lambda$12758/0x00007f6e46003688@53b8e86a,
//   event = cats.effect.tracing.TracingEvent$StackTrace
// )
```

## Java nullable values

Use `.nullable` at a Java API boundary to turn a null into None:

```scala
val javaValues = new java.util.HashMap[String, String]()
// javaValues: HashMap[String, String] = {present=hello}
javaValues.put("present", "hello")
// res0: String = null
val absent = javaValues.get("missing").nullable
// absent: Option[String] = None
val present = javaValues.get("present").nullable
// present: Option[String] = Some("hello")
```

The results are `None` and `Some("hello")`. This helper is available through
`common`, `core`, `all`, or the focused `pspsps.syntax.nullable.*` import.
It accepts reference types, including boxed Java values, and excludes Scala primitives.

## Conditional values and effects

Use the factory namespaces fully qualified alongside the normal Scala and Cats
companion imports:

```scala
val conditionalEither: Either[String, Int] =
  pspsps.builders.Either.cond(true).withLeft("missing").withRight(42)
// conditionalEither: Either[String, Int] = Right(42)
val conditionalOption: Option[Int] =
  pspsps.builders.Option.cond(true).withSome(42)
// conditionalOption: Option[Int] = Some(42)
val conditionalEitherT: EitherT[IO, String, Int] =
  pspsps.builders.EitherT.cond(IO.pure(true)).withLeft("missing").withRight(IO.pure(42))
// conditionalEitherT: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$12221/0x00007f6e45efcb80@2272f587,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
val conditionalOptionT: OptionT[IO, Int] =
  pspsps.builders.OptionT.cond(IO.pure(true)).withSome(IO.pure(42))
// conditionalOptionT: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$12221/0x00007f6e45efcb80@73b5bbaa,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
val conditionalPureTest: EitherT[IO, String, Int] =
  pspsps.builders.EitherT.cond[IO](true).withRight(42).withLeft(IO.pure("missing"))
// conditionalPureTest: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithRight$$Lambda$12771/0x00007f6e46008840@1c8d3e0,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

assert(conditionalEither == Right(42))
assert(conditionalOption == Some(42))
assert(pspsps.builders.Option.cond(false).withSome[Int](sys.error("unselected")) == None)
```

The postfix spelling needs only the normal syntax import:

```scala
val postfixEither: Either[String, Int] =
  true.asEitherCond.withLeft("missing").withRight(42)
// postfixEither: Either[String, Int] = Right(42)
val postfixOption: Option[Int] =
  true.asOptionCond.withSome(42)
// postfixOption: Option[Int] = Some(42)
val postfixEitherT: EitherT[IO, String, Int] =
  IO.pure(true).asEitherTCond.withLeft("missing").withRight(IO.pure(42))
// postfixEitherT: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$12221/0x00007f6e45efcb80@1527900b,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
val postfixOptionT: OptionT[IO, Int] =
  true.asOptionTCond[IO].withSome(42)
// postfixOptionT: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.OptionCondition$$Lambda$12230/0x00007f6e45effae0@60539a78,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

Only the selected branch is evaluated. A Boolean condition selects at builder
completion; an `IO[Boolean]` condition selects inside IO execution. Plain
Option/Either do not run effects: use OptionT/EitherT for an effectful condition.
An explicitly pure transformer branch can retain an effect as the payload:

```scala
val storedEffect: OptionT[IO, IO[Int]] =
  pspsps.builders.OptionT.condPure[IO](true).withSomePure(IO.pure(42))
// storedEffect: OptionT[[A >: Nothing <: Any] => IO[A], IO[Int]] = OptionT(
//   Map(
//     ioe = Pure(Pure(42)),
//     f = pspsps.builders.OptionCondition$$Lambda$12230/0x00007f6e45effae0@60539a78,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

## Continue reading

[Modules and imports](modules.md) covers dependency choices and focused syntax.
[Conditional builders](conditionals.md) documents branch inference and evaluation.
[Evaluation and resource safety](semantics.md) explains ownership and output shapes.
[API documentation](api-reference.md) contains the full Scaladoc.
