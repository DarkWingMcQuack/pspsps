# Fluent constructors for the Cats ecosystem

pspsps adds postfix constructors to Cats, Cats Effect, and FS2. Every operation
returns the upstream datatype and preserves its evaluation and error semantics.

Start with [[pspsps.syntax.common]] for everyday pipelines or
[[pspsps.syntax.all]] for the complete set of constructors.

## Explore the API

- [[pspsps.syntax.CoreSyntax]]: Cats transformers, conditionals, and nullable Java references.
- [[pspsps.syntax.EffectSyntax]]: Cats Effect Resource and Ref constructors.
- [[pspsps.syntax.Fs2Syntax]]: FS2 Stream, Pull, and Chunk constructors.
- [[pspsps.builders]]: staged Either, Option, EitherT, and OptionT conditional factories.
- [[pspsps.laws]]: reusable constructor and conditional coherence checks.
