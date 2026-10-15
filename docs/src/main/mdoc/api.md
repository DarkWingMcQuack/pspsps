# API documentation

[Browse the complete Scaladoc][api] for searchable types, extension methods,
conditional builders, and reusable laws across all pspsps modules.

## Find a constructor

| Area | API entry point | What it provides |
| --- | --- | --- |
| Everyday imports | [`common`][common] | Curated transformer, conditional, resource, reference, stream, and chunk syntax |
| Complete imports | [`all`][all] | Every constructor family |
| Cats | [`CoreSyntax`][core] | OptionT, EitherT, IorT, WriterT, Kleisli, StateT, conditionals, and Java nullable values |
| Cats Effect | [`EffectSyntax`][effect] | Resource and Ref constructors |
| FS2 | [`Fs2Syntax`][fs2] | Stream, Pull, and Chunk constructors |
| Conditional builders | [`pspsps.builders`][builders] | Staged Either, Option, EitherT, and OptionT factories |
| Laws | [`pspsps.laws`][laws] | Constructor and conditional coherence laws with Discipline checks |

## Read extension methods

Open the syntax trait for a constructor family to see its extension methods,
receiver types, type parameters, and required typeclass instances. The `common`
and `all` objects collect those methods for importing into application code.

The [semantics guide](semantics.md) explains evaluation, failure, and resource
handling; [conditional builders](conditionals.md) covers staged construction
and type inference with checked examples.

[api]: api/index.html
[common]: api/pspsps/syntax/common$.html
[all]: api/pspsps/syntax/all$.html
[core]: api/pspsps/syntax/CoreSyntax.html
[effect]: api/pspsps/syntax/EffectSyntax.html
[fs2]: api/pspsps/syntax/Fs2Syntax.html
[builders]: api/pspsps/builders.html
[laws]: api/pspsps/laws.html
