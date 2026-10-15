# Conditional builders

Staged, by-name constructors for **Either, Option, EitherT, and OptionT**.
The final stage returns a standard Scala or Cats value, not a new wrapper.
These builders live in `pspsps-core` and add no dependencies.

## Factory style

```scala
import cats.effect.IO
import cats.syntax.all.*
import pspsps.builders.{Either, EitherT, Option, OptionT}

val pureEither: Either[String, Int] =
  Either.cond(true).withLeft("missing").withRight(42)
// pureEither: Either[String, Int] = Right(42)

val pureOption: Option[Int] =
  Option.cond(true).withSome(42)
// pureOption: Option[Int] = Some(42)

val checked: EitherT[IO, String, Int] =
  EitherT.cond(true.pure[IO])
    .withLeft("missing")
    .withRight(42.pure[IO])
// checked: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@2e9ff51a,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

val optional: OptionT[IO, Int] =
  OptionT.cond(true.pure[IO]).withSome(42.pure[IO])
// optional: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@1cba1909,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

// A pure Boolean does not identify an outer effect: supply F explicitly.
val pureTest: EitherT[IO, String, Int] =
  EitherT.cond[IO](true).withLeft("missing").withRight(42)
// pureTest: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

val pureOptionTest: OptionT[IO, Int] =
  OptionT.cond[IO](true).withSome(42)
// pureOptionTest: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.OptionCondition$$Lambda$13207/0x00007f365200d3e0@4f6c39eb,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

The `pspsps.builders` names are **opt-in factory namespaces** returning standard
Scala and Cats result types. They are not extensions that replace existing members
of the original companions. Scala already has `Either.cond(test, right, left)`;
Cats already has `EitherT.cond[F](test, right, left)`.

Avoid importing both Cats' and pspsps' `EitherT`/`OptionT` under the same
name. Keep the native constructors qualified, alias the builder imports, or use
the postfix API below. For example:

```scala
import cats.effect.IO
import pspsps.builders.{EitherT as EitherTBuilder}

val aliased = EitherTBuilder.cond[IO](true).withLeft("missing").withRight(42)
// aliased: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

The factory namespaces provide staged `cond` construction only. Use the normal
Scala and Cats companions for their other constructors. Normal imports from
`pspsps.syntax` do not shadow any companion names.

Scala 2.13 uses `_` wildcard imports and `=>` aliases; Scala 3 examples here
use `*` and `as`. Both versions support the same staged branch API.

## Postfix style

```scala
import cats.effect.IO
import cats.syntax.all.*
import pspsps.syntax.common.*

val a: Either[String, Int] =
  true.asEitherCond.withLeft("missing").withRight(42)
// a: Either[String, Int] = Right(42)

val b: Option[Int] =
  true.asOptionCond.withSome(42)
// b: Option[Int] = Some(42)

val c: cats.data.EitherT[IO, String, Int] =
  true.pure[IO].asEitherTCond.withLeft("missing").withRight(42.pure[IO])
// c: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@4fb6f05d,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

val d: cats.data.OptionT[IO, Int] =
  true.pure[IO].asOptionTCond.withSome(42.pure[IO])
// d: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@308caf3,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

val e: cats.data.EitherT[IO, String, Int] =
  true.asEitherTCond[IO].withLeft("missing").withRight(42)
// e: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

val f: cats.data.OptionT[IO, Int] =
  true.asOptionTCond[IO].withSome(42)
// f: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.OptionCondition$$Lambda$13207/0x00007f365200d3e0@4f6c39eb,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

Focused imports are `pspsps.syntax.conditional.*` for all four families,
`pspsps.syntax.either.*` / `option.*` for the pure families, and the existing
`pspsps.syntax.eitherT.*` / `optionT.*` for the transformer families.
`pspsps.syntax.core.*` also includes all four.

## Selection and staging

| Builder | Condition | False | True | Result |
| --- | --- | --- | --- | --- |
| `Either` | `Boolean` | `Left(left)` | `Right(right)` | `Either[A, B]` |
| `Option` | `Boolean` | `None` | `Some(some)` | `Option[A]` |
| `EitherT` | `Boolean` or `F[Boolean]` | Left branch in F | Right branch in F | `EitherT[F, A, B]` |
| `OptionT` | `Boolean` or `F[Boolean]` | `F.pure(None)` | Some branch in F | `OptionT[F, A]` |

Either branches can be supplied in either order. **True always selects Right**:

```scala
import pspsps.builders.Either

val a = Either.cond(true).withLeft("no").withRight(42)
// a: Either[String, Int] = Right(42)
val b = Either.cond(true).withRight(42).withLeft("no")
// b: Either[String, Int] = Right(42)
assert(a == b)
```

The same holds for EitherT. An intermediate Either builder is not an Either or
EitherT, does not expose `.value`, and accepts only its missing branch. Option
builders complete at `.withSome`: None has no payload, so there is no artificial
`.withNone` stage. Builders are immutable and reusable, not memoized or consumed.

## Pure/effectful branch matrix

EitherT accepts all eight combinations of pure/effectful test, left, and right.
Every row also works with Right supplied first:

```scala
import cats.effect.IO
import cats.syntax.all.*
import pspsps.builders.EitherT

EitherT.cond[IO](true).withLeft("no").withRight(42)
// res6: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond[IO](true).withLeft("no".pure[IO]).withRight(42)
// res7: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond[IO](true).withLeft("no").withRight(42.pure[IO])
// res8: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond[IO](true).withLeft("no".pure[IO]).withRight(42.pure[IO])
// res9: EitherT[[A >: Nothing <: Any] => IO[A], String, Int] = EitherT(
//   Map(
//     ioe = Pure(42),
//     f = pspsps.builders.EitherWithLeft$$Lambda$13205/0x00007f365200d018@71fe7879,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond(true.pure[IO]).withLeft("no").withRight(42)
// res10: EitherT[IO, String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@137475b,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond(true.pure[IO]).withLeft("no".pure[IO]).withRight(42)
// res11: EitherT[IO, String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@2fba85f7,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond(true.pure[IO]).withLeft("no").withRight(42.pure[IO])
// res12: EitherT[IO, String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@57f6ac45,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
EitherT.cond(true.pure[IO]).withLeft("no".pure[IO]).withRight(42.pure[IO])
// res13: EitherT[IO, String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@12ecab1a,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

OptionT accepts the analogous four combinations of test and Some payload.
Automatic branch interpretation removes exactly one layer of the **chosen F**:
`IO[Int]` becomes an `Int` payload, and `IO[IO[Int]]` becomes an `IO[Int]` payload.
A value in a different context, such as `List[Int]` when F is IO, is just a pure
List payload. It is not automatically executed or converted into IO.

### Explicit escape hatches

Automatic branch methods use the sealed `Branch[F, Input, A]` typeclass.
The pure instance is excluded when the input has the shape of F. There are no
runtime type tests, casts, unsafe effect runners, or global implicit conversions.

The `...F` methods explicitly accept `F[A]`; the `...Pure` methods explicitly
retain A as the payload. These avoid ambiguity and provide useful expected types
for generic code, effect subtypes, `Nothing`, null, and intentionally nested
payloads:

```scala
import cats.effect.IO
import cats.syntax.all.*
import pspsps.builders.{EitherT, OptionT}

EitherT.condF(true.pure[IO])
  .withLeftPure("missing")
  .withRightF(42.pure[IO])
// res15: EitherT[IO, String, Int] = EitherT(
//   FlatMap(
//     ioe = Pure(true),
//     f = pspsps.builders.Decision$$anon$2$$Lambda$13198/0x00007f3652006410@295a3b83,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )

// Store IO[Int] as the payload, instead of interpreting it as the outer effect.
val nested: cats.data.OptionT[IO, IO[Int]] =
  OptionT.condPure[IO](true).withSomePure(42.pure[IO])
// nested: OptionT[[A >: Nothing <: Any] => IO[A], IO[Int]] = OptionT(
//   Map(
//     ioe = Pure(Pure(42)),
//     f = pspsps.builders.OptionCondition$$Lambda$13207/0x00007f365200d3e0@4f6c39eb,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

Explicit spellings are `condPure[F]`, `condF`, `withLeftPure`, `withLeftF`,
`withRightPure`, `withRightF`, `withSomePure`, and `withSomeF`. Postfix variants
include `asEitherTCondPure[F]`, `asEitherTCondF`, `asOptionTCondPure[F]`, and
`asOptionTCondF`. They are particularly useful with `cats.Id`, where `F[A]` and A
are the same type.

Automatic input inference is deliberately invariant. Widen effect subclasses
such as `Some[A]` to `Option[A]` first, or provide the expected type through the
explicit F method:

```scala
import cats.instances.option.*
import pspsps.builders.OptionT

val result: cats.data.OptionT[Option, Int] =
  OptionT.condPure[Option](true).withSomeF(Some(42))
// result: OptionT[[A >: Nothing <: Any] => Option[A], Int] = OptionT(
//   Some(Some(42))
// )
```

## Evaluation contract

**Unselected branch expressions are never evaluated by the builder.** Both
pure payload expressions and effect factories are by-name. The initial Either
stage stores a thunk and does not evaluate its first branch even when that
branch will ultimately be selected.

**A Boolean condition is strict.** It is evaluated once when the builder is
created. The selected branch expression is evaluated when the final stage
completes; there is no promise that it is delayed until IO execution. The IO
returned by a selected effect factory still follows IO's own evaluation rules.

**An effectful condition uses F.flatMap.** For IO, the condition and selected
branch are sequenced inside the returned IO. Re-running the returned IO
re-runs its condition and selected branch; the builder introduces no memoization.
For a multi-valued effect such as List, every Boolean result makes its own
selection in the normal flatMap order. An empty outer effect remains empty.
Do not generalize an IO-specific "once per run" description to every F.

**No automatic error-channel conversion.** Failures or cancellation of the
condition or selected effect stay in F; they do not become domain Left or None.
A selected throwing plain expression is not caught by the pure constructors.
Use the effect's own suspension and error handling explicitly. An already
started Future cannot be unstarted by passing it as a by-name argument.

**Some means Some.** `Option.cond(true).withSome(null)` is Some(null), matching
`scala.Option.when`, not None. The OptionT variant follows the same Some/None
choice. Null-elimination is a different operation.

## Plain values are not an effect runner

The plain factories accept Boolean, not `F[Boolean]`. Their payloads are ordinary
values; an effect can itself be such a value:

```scala
import cats.effect.IO
import cats.syntax.all.*
import pspsps.builders.{Either, Option}

val a: Either[String, IO[Int]] =
  Either.cond(true).withLeft("missing").withRight(42.pure[IO])
// a: Either[String, IO[Int]] = Right(Pure(42))
val b: Option[IO[Int]] =
  Option.cond(true).withSome(42.pure[IO])
// b: Option[IO[Int]] = Some(Pure(42))
```

For an effectful condition, use EitherT or OptionT. Their `.value` returns
`F[Either[A, B]]` or `F[Option[A]]` without executing F. There is no lawful
universal operation that extracts a plain value from an arbitrary F here.

## Constraints

The explicit methods expose these minimal requirements:

| Operation | Requirement |
| --- | --- |
| Plain Either/Option builders | None |
| Start a transformer builder from Boolean | None |
| Start a transformer builder from `F[Boolean]` | `FlatMap[F]` |
| Complete EitherT with two explicit F branches | `Functor[F]` |
| Lift a pure transformer branch | `Applicative[F]` |
| Complete OptionT, including its empty branch | `Applicative[F]` |

An effectful condition plus a pure branch generally means using a `Monad[F]`
instance, which supplies both capabilities. The implementation requests them
where used rather than imposing Sync or Monad on every entry point.
