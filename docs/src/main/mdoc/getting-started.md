# Getting started

Postfix constructors for Cats, Cats Effect, and FS2 in Scala 2.13 and Scala 3.
Scala 2.13 uses `_` in place of `*` for wildcard imports.

## One import

```scala mdoc
import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.instances.list.*
import cats.syntax.option.*
import pspsps.syntax.common.*
import fs2.Stream

val input: IO[Option[Int]] = IO.pure(Some(42))
val wrapped: OptionT[IO, Int] = input.asOptionT
val lifted: OptionT[IO, Int] = Option(42).asOptionT[IO]
val plainEither: Either[String, Int] = Right(42)
val either: EitherT[IO, String, Int] = plainEither.asEitherT[IO]
val evaluated: Stream[IO, Int] = IO.pure(42).asStreamEval
```

## A complete pipeline

The IO actions execute when the stream runs.

```scala mdoc
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
```

## Wrapping, lifting, and pure values are different

```scala mdoc
val nested: Option[Option[Int]] = Some(Some(42))
val wrapNested: OptionT[Option, Int] = nested.asOptionTWrap
val liftNested: OptionT[IO, Option[Int]] = nested.toOptionT[IO]
val liftEffect: EitherT[IO, String, Int] = IO.pure(42).asEitherTLiftF[String]
val pureValue: EitherT[IO, String, Int] = 42.asEitherTPure[IO, String]

val captureErrors: EitherT[IO, Throwable, Int] =
  IO.raiseError[Int](new RuntimeException("example"))
    .attempt
    .asEitherT
```

`asEitherTLiftF` sends successful effect results to `Right`. It does not catch
errors of the outer effect. `attempt.asEitherT` makes that policy explicit.

## Collection output versus singleton output

```scala mdoc
val oneCollection: Stream[IO, List[Int]] = List(1, 2, 3).asStreamEmit[IO]
val threeElements: Stream[IO, Int] = List(1, 2, 3).asStreamEmits[IO]
val evaluatedElements: Stream[IO, Int] = IO.pure(List(1, 2, 3)).asStreamEvals
val noElements: Stream[IO, Nothing] = IO.unit.asStreamExec
```

`asStreamEmits` and `asStreamEvals` are for finite collections.
`asStreamEvals` follows FS2's conversion of the Foldable to a List.
Neither method promises streaming traversal of an infinite collection.

## Resource ownership

```scala mdoc
val acquire: IO[String] = IO.pure("connection")
val release: String => IO[Unit] = _ => IO.unit
val scoped = acquire.asResourceMake(release).asStreamResource
val unowned = acquire.asResourceEval
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

```scala mdoc
import pspsps.syntax.kleisli.*
import pspsps.syntax.writerT.*
import pspsps.syntax.stateT.*
val readLength: String => IO[Int] = key => IO.pure(key.length)
val lookup = readLength.asKleisli
val logAndResult = (List("read"), 42).asWriterTFromTuple[IO]
val stateTransition: Int => IO[(Int, String)] = n => IO.pure((n + 1, n.toString))
val state = stateTransition.asStateT
val initialRef = 0.asRef[IO]
```

## Java nullable values

Use `.nullable` at a Java API boundary to turn a null into None:

```scala mdoc
val javaValues = new java.util.HashMap[String, String]()
javaValues.put("present", "hello")
val absent = javaValues.get("missing").nullable
val present = javaValues.get("present").nullable
```

The results are `None` and `Some("hello")`. This helper is available through
`common`, `core`, `all`, or the focused `pspsps.syntax.nullable.*` import.
It accepts reference types, including boxed Java values, and excludes Scala primitives.

## Conditional values and effects

Use the factory namespaces fully qualified alongside the normal Scala and Cats
companion imports:

```scala mdoc
val conditionalEither: Either[String, Int] =
  pspsps.builders.Either.cond(true).withLeft("missing").withRight(42)
val conditionalOption: Option[Int] =
  pspsps.builders.Option.cond(true).withSome(42)
val conditionalEitherT: EitherT[IO, String, Int] =
  pspsps.builders.EitherT.cond(IO.pure(true)).withLeft("missing").withRight(IO.pure(42))
val conditionalOptionT: OptionT[IO, Int] =
  pspsps.builders.OptionT.cond(IO.pure(true)).withSome(IO.pure(42))
val conditionalPureTest: EitherT[IO, String, Int] =
  pspsps.builders.EitherT.cond[IO](true).withRight(42).withLeft(IO.pure("missing"))

assert(conditionalEither == Right(42))
assert(conditionalOption == Some(42))
assert(pspsps.builders.Option.cond(false).withSome[Int](sys.error("unselected")) == None)
```

The postfix spelling needs only the normal syntax import:

```scala mdoc
val postfixEither: Either[String, Int] =
  true.asEitherCond.withLeft("missing").withRight(42)
val postfixOption: Option[Int] =
  true.asOptionCond.withSome(42)
val postfixEitherT: EitherT[IO, String, Int] =
  IO.pure(true).asEitherTCond.withLeft("missing").withRight(IO.pure(42))
val postfixOptionT: OptionT[IO, Int] =
  true.asOptionTCond[IO].withSome(42)
```

Only the selected branch is evaluated. A Boolean condition selects at builder
completion; an `IO[Boolean]` condition selects inside IO execution. Plain
Option/Either do not run effects: use OptionT/EitherT for an effectful condition.
An explicitly pure transformer branch can retain an effect as the payload:

```scala mdoc
val storedEffect: OptionT[IO, IO[Int]] =
  pspsps.builders.OptionT.condPure[IO](true).withSomePure(IO.pure(42))
```

## Continue reading

[Modules and imports](modules.md) covers dependency choices and focused syntax.
[Conditional builders](conditionals.md) documents branch inference and evaluation.
[Evaluation and resource safety](semantics.md) explains ownership and output shapes.
[API documentation](api.md) contains the full Scaladoc.
