# Laws

pspsps is a syntax library. Its central law is **coherence with the
constructor it names**: wrapping, lifting, mapping, and conditional selection
must preserve the upstream representation and semantics.

## Constructor laws

The separate `pspsps-laws` artifact exposes reusable `IsEq` laws and
Discipline rules for OptionT and EitherT. Application runtime modules do not
bring ScalaCheck or Discipline into their dependency graphs.

```scala mdoc
import cats.instances.option.*
import cats.syntax.all.*
import pspsps.laws.ConstructorLaws

val constructors = ConstructorLaws[Option]
val input: Option[Option[Int]] = 42.some.some
val roundTrip = constructors.optionWrap(input)
val mapping = constructors.optionMap(input, (value: Int) => value + 1)
```

Each law returns its two sides as a Cats `IsEq`. The rulesets check:

| Rule | Contract |
| --- | --- |
| Wrap round trip | `.value` returns the original representation |
| From-inner coherence | Lifting agrees with `Applicative.pure` |
| LiftF coherence | Effect lifting agrees with the Cats constructor |
| Pure coherence | Value lifting agrees with the Cats constructor |
| Map coherence | Transformer mapping agrees with mapping the representation |

## Conditional laws

Staged conditionals have reusable laws in `ConditionalLaws[F]`, requiring
`Monad[F]`. They agree with Cats' `ifM` branch selection, preserve branch order
independence, and support a complement law: negating the condition and swapping
both branches and Either channels gives the same result.

`OptionT` conditionals agree with lifting the selected Some branch or producing
None. Empty or failed outer conditions remain empty or failed; the builder does
not convert an outer failure into a domain Left or None.

`List` makes the effect's multiplicity visible. Each Boolean result chooses
its branch, retaining the normal flatMap order:

```scala mdoc
import cats.instances.list.*
import pspsps.laws.ConditionalLaws

val conditional = ConditionalLaws[List]
val selected = conditional.eitherCondition(List(true, false), List("missing"), List(42))
selected.lhs
selected.rhs

val ordered = conditional.eitherBranchOrder(List(true, false), List("missing"), List(42))
val complemented = conditional.eitherComplement(List(true, false), List("missing"), List(42))
ordered.lhs
complemented.lhs
```

All four results are `List(Right(42), Left("missing"))`. The original order
of supplied Either branches changes neither selection nor the effect's order.
For Option, an absent condition produces no outer result:

```scala mdoc
val absent = ConditionalLaws[Option].optionCondition(none[Boolean], 42.some)
absent.lhs
```

For Either, the error of a failed condition stays in the outer effect:

```scala mdoc
import cats.instances.either.*

type ErrorOr[A] = Either[String, A]
val failedCondition: ErrorOr[Boolean] = "condition failed".asLeft[Boolean]
val failed = ConditionalLaws[ErrorOr].eitherCondition[String, Int](
  failedCondition,
  "missing".asRight[String],
  42.asRight[String]
)
failed.lhs
```

The result is `Left("condition failed")` in the outer ErrorOr, rather than
`Right(Left("missing"))` in the transformer's domain channel.

## Discipline integration

Create rulesets for the outer effect you want to check:

```scala mdoc
import pspsps.laws.discipline.{ConditionalTests, ConstructorTests}

val optionRules = ConstructorTests[Option].optionT[Int]
val eitherRules = ConstructorTests[Option].eitherT[String, Int]
val listConditionalRules = ConditionalTests[List].eitherT[String, Int]
val optionalConditionalRules = ConditionalTests[Option].optionT[Int]
val errorConditionalRules = ConditionalTests[ErrorOr].eitherT[String, Int]
```

Register these using Discipline MUnit's `checkAll`. Provide Applicative or
Monad, ScalaCheck Arbitrary, and Cats Eq instances for your effect.
