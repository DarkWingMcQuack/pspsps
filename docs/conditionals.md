# Conditional builders

The complete guide now lives in the compile-checked microsite sources:

**[Conditional builders: imports, branch matrix, evaluation, and constraints](src/main/mdoc/conditionals.md)**

This keeps the published documentation and compiler-checked examples together.
Run `sbt docs/mdoc` to check the examples, or `sbt 'docs/tlSitePreview'` to read
the rendered guide at `http://localhost:4242`.

pspsps provides staged, by-name constructors for standard Either and Option
and Cats EitherT and OptionT. True selects Right or Some; the unselected branch
is never evaluated by the builder. Both Either branch orders are supported.
The opt-in factory names live in `pspsps.builders`; normal syntax imports also
provide postfix entry points without shadowing upstream companions.

```scala
import cats.effect.IO
import pspsps.syntax.common.*

true.asEitherCond.withLeft("missing").withRight(42)
true.asOptionCond.withSome(42)
IO.pure(true).asEitherTCond.withLeft("missing").withRight(IO.pure(42))
IO.pure(true).asOptionTCond.withSome(IO.pure(42))
```

## Source and tests

- [Transformer builders](../modules/core/src/main/scala-3/pspsps/builders/ConditionalBuilders.scala)
- [Pure builders](../modules/core/src/main/scala-3/pspsps/builders/PureConditionalBuilders.scala)
- [Postfix syntax](../modules/core/src/main/scala-3/pspsps/syntax/ConditionalSyntax.scala)
- [Scala 2 transformer semantics](../tests/src/test/scala-2/pspsps/ConditionalSuite.scala)
- [Scala 3 transformer semantics](../tests/src/test/scala-3/pspsps/ConditionalSuite.scala)
- [Transformer compiler contracts](../tests/src/test/scala-3/pspsps/ConditionalCompileSuite.scala)
- [Pure properties and semantics](../tests/src/test/scala/pspsps/PureConditionalSuite.scala)
- [Pure compiler contracts](../tests/src/test/scala-3/pspsps/PureConditionalCompileSuite.scala)

The tests run in the shared JVM/Scala.js suite. See
[verification status](../VERIFICATION.md) for recorded execution results.
Upstream constructor references are recorded in [dependencies](dependencies.md).
