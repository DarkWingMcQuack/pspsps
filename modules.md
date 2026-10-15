# Modules and imports

Start with the umbrella artifact and `pspsps.syntax.common.*` for everyday
pipelines. Use individual modules for a smaller dependency footprint.
All library modules cross-compile for Scala 2.13 and Scala 3, on JVM and Scala.js.
The examples here use Scala 3 imports; Scala 2.13 replaces `*` with `_`.

| Artifact | Aggregate import | Direct dependency |
| --- | --- | --- |
| `pspsps-core` | `pspsps.syntax.core.*` | Cats Core |
| `pspsps-effect` | `pspsps.syntax.effect.*` | Cats Effect Kernel |
| `pspsps-fs2` | `pspsps.syntax.streaming.*` | FS2 Core |
| `pspsps` | `pspsps.syntax.common.*` or `all.*` | Core, effect, and FS2 syntax |
| `pspsps-laws` | `pspsps.laws.discipline` | Core and law-testing libraries |

The three syntax modules are independent. FS2's normal transitive dependencies
still include Cats and Cats Effect. Both `common` and `all` live in the umbrella
artifact.

## Curated or complete

`common` includes OptionT, EitherT, conditional builders, basic Resource
construction, Ref, Stream, Chunk, and `.nullable`. It is the recommended starting
import for application code.

`all` includes the complete syntax surface: common plus IorT, WriterT,
Reader/Kleisli/Cokleisli, State/StateT, Pull, Resource interop, and advanced
Resource acquisition. You can also combine common with focused specialized
imports.

## Focused imports

| Family | Import | Examples |
| --- | --- | --- |
| OptionT | `pspsps.syntax.optionT.*` | `asOptionTWrap`, `asOptionTLiftF` |
| EitherT | `pspsps.syntax.eitherT.*` | `asEitherTWrap`, `asEitherTLiftF` |
| Conditional builders | `pspsps.syntax.conditional.*` | `asEitherCond`, `asOptionTCond` |
| Nullable Java values | `pspsps.syntax.nullable.*` | `nullable` |
| IorT | `pspsps.syntax.iorT.*` | `asIorT`, `asIorTLiftF` |
| Writer / WriterT | `pspsps.syntax.writerT.*` | `asWriterT`, `asWriterTFromTuple` |
| Reader / Kleisli / Cokleisli | `pspsps.syntax.kleisli.*` | `asKleisli`, `asReader` |
| State / StateT | `pspsps.syntax.stateT.*` | `asStateT`, `asIndexedStateT` |
| Complete Resource family | `pspsps.syntax.resource.*` | Basic, interop, and advanced constructors |
| Resource interop | `pspsps.syntax.resourceInterop.*` | `asResourceWrap`, `asResourceWrapCase` |
| Advanced Resource acquisition | `pspsps.syntax.resourceAdvanced.*` | `asResourceMakeFull`, `asResourceMakeCaseFull` |
| Ref | `pspsps.syntax.ref.*` | `asRef`, `asRefOfEffect` |
| Stream | `pspsps.syntax.stream.*` | `asStreamEval`, `asStreamEmits` |
| Pull | `pspsps.syntax.pull.*` | `asPullEval`, `asPullOutput1` |
| Chunk | `pspsps.syntax.chunk.*` | `asChunkSingleton`, `asChunk` |

Choose one aggregate, or the focused families you need. Importing the same
family through several aggregates can make extension resolution ambiguous.
Cats' usual syntax remains available alongside pspsps.

```scala
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.all.*
import pspsps.syntax.common.*

val optional: OptionT[IO, Int] = 42.some.asOptionT[IO]
// optional: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Pure(Some(42))
// )
val increased: OptionT[IO, Int] = optional.map(_ + 1)
// increased: OptionT[[A >: Nothing <: Any] => IO[A], Int] = OptionT(
//   Map(
//     ioe = Pure(Some(42)),
//     f = cats.data.OptionT$$Lambda$12809/0x00007fce6dfdc278@1bb6e54a,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   )
// )
```

## Naming and nested shapes

| Operation | Meaning | Example |
| --- | --- | --- |
| `Wrap` | Wrap the existing outer representation | `fa.asOptionTWrap` |
| Explicit effect parameter | Lift an inner Option or Either | `opt.asOptionT[F]` |
| `LiftF` | Lift successful values from an existing effect | `fa.asEitherTLiftF[E]` |
| `Pure` | Lift an already-computed value | `a.asOptionTPure[F]` |
| `Eval` | Evaluate an effect in the target datatype | `fa.asStreamEval` |
| `Emit` / `Emits` | Emit one value / collection elements | `xs.asStreamEmits[F]` |
| `Make` | Pair resource acquisition and release | `acquire.asResourceMake(release)` |

For nested shapes, `asOptionTWrap` and `asEitherTWrap` explicitly select wrapping.
Cats' `toOptionT` and `toEitherT` explicitly select lifting an inner Option or
Either. Use them when an overloaded short spelling would be ambiguous.

Browse [API documentation](api-reference.md) for all types and extension methods.
