# Laws

pspsps is a syntax library. Its central law is **coherence with the
constructor it names**: wrapping, lifting, mapping, and conditional selection
must preserve the upstream representation and semantics.

## Constructor laws

The separate `pspsps-laws` artifact exposes reusable `IsEq` laws and
Discipline rules for OptionT and EitherT. Application runtime modules do not
bring ScalaCheck or Discipline into their dependency graphs.

```scala
import cats.instances.option.*
import cats.syntax.all.*
import pspsps.laws.ConstructorLaws

val constructors = ConstructorLaws[Option]
// constructors: ConstructorLaws[[A >: Nothing <: Any] => Option[A]] = pspsps.laws.ConstructorLaws@7532a44f
val input: Option[Option[Int]] = 42.some.some
// input: Option[Option[Int]] = Some(Some(42))
val roundTrip = constructors.optionWrap(input)
// roundTrip: IsEq[Option[Option[Int]]] = IsEq(
//   lhs = Some(Some(42)),
//   rhs = Some(Some(42))
// )
val mapping = constructors.optionMap(input, (value: Int) => value + 1)
// mapping: IsEq[Option[Option[Int]]] = IsEq(
//   lhs = Some(Some(43)),
//   rhs = Some(Some(43))
// )
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

```scala
import cats.instances.list.*
import pspsps.laws.ConditionalLaws

val conditional = ConditionalLaws[List]
// conditional: ConditionalLaws[List] = pspsps.laws.ConditionalLaws@f4639e7
val selected = conditional.eitherCondition(List(true, false), List("missing"), List(42))
// selected: IsEq[List[Either[String, Int]]] = IsEq(
//   lhs = List(Right(42), Left("missing")),
//   rhs = List(Right(42), Left("missing"))
// )
selected.lhs
// res0: List[Either[String, Int]] = List(Right(42), Left("missing"))
selected.rhs
// res1: List[Either[String, Int]] = List(Right(42), Left("missing"))

val ordered = conditional.eitherBranchOrder(List(true, false), List("missing"), List(42))
// ordered: IsEq[List[Either[String, Int]]] = IsEq(
//   lhs = List(Right(42), Left("missing")),
//   rhs = List(Right(42), Left("missing"))
// )
val complemented = conditional.eitherComplement(List(true, false), List("missing"), List(42))
// complemented: IsEq[List[Either[String, Int]]] = IsEq(
//   lhs = List(Right(42), Left("missing")),
//   rhs = List(Right(42), Left("missing"))
// )
ordered.lhs
// res2: List[Either[String, Int]] = List(Right(42), Left("missing"))
complemented.lhs
// res3: List[Either[String, Int]] = List(Right(42), Left("missing"))
```

All four results are `List(Right(42), Left("missing"))`. The original order
of supplied Either branches changes neither selection nor the effect's order.
For Option, an absent condition produces no outer result:

```scala
val absent = ConditionalLaws[Option].optionCondition(none[Boolean], 42.some)
// absent: IsEq[Option[Option[Int]]] = IsEq(lhs = None, rhs = None)
absent.lhs
// res4: Option[Option[Int]] = None
```

For Either, the error of a failed condition stays in the outer effect:

```scala
import cats.instances.either.*

type ErrorOr[A] = Either[String, A]
val failedCondition: ErrorOr[Boolean] = "condition failed".asLeft[Boolean]
// failedCondition: Either[String, Boolean] = Left("condition failed")
val failed = ConditionalLaws[ErrorOr].eitherCondition[String, Int](
  failedCondition,
  "missing".asRight[String],
  42.asRight[String]
)
// failed: IsEq[ErrorOr[Either[String, Int]]] = IsEq(
//   lhs = Left("condition failed"),
//   rhs = Left("condition failed")
// )
failed.lhs
// res5: Either[String, Either[String, Int]] = Left("condition failed")
```

The result is `Left("condition failed")` in the outer ErrorOr, rather than
`Right(Left("missing"))` in the transformer's domain channel.

## Discipline integration

Create rulesets for the outer effect you want to check:

```scala
import pspsps.laws.discipline.{ConditionalTests, ConstructorTests}

val optionRules = ConstructorTests[Option].optionT[Int]
// optionRules: RuleSet = org.typelevel.discipline.Laws$DefaultRuleSet@6853370d
val eitherRules = ConstructorTests[Option].eitherT[String, Int]
// eitherRules: RuleSet = org.typelevel.discipline.Laws$DefaultRuleSet@50e60b44
val listConditionalRules = ConditionalTests[List].eitherT[String, Int]
// listConditionalRules: RuleSet = org.typelevel.discipline.Laws$DefaultRuleSet@32a271b3
val optionalConditionalRules = ConditionalTests[Option].optionT[Int]
// optionalConditionalRules: RuleSet = org.typelevel.discipline.Laws$DefaultRuleSet@74825dd4
val errorConditionalRules = ConditionalTests[ErrorOr].eitherT[String, Int]
// errorConditionalRules: RuleSet = org.typelevel.discipline.Laws$DefaultRuleSet@5898699f
```

Register these using Discipline MUnit's `checkAll`. Provide Applicative or
Monad, ScalaCheck Arbitrary, and Cats Eq instances for your effect.
