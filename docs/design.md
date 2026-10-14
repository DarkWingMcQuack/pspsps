# Design decisions

## Purpose

Keep left-to-right composition intact at datatype-construction boundaries.
This is a syntax library, not an effect framework, a prelude replacing Cats,
or an attempt to attach every imaginable verb to every Scala value.

## Direct extensions, not a universal conversion typeclass

Each ordinary postfix constructor has an explicit receiver and return type and delegates to
the relevant upstream constructor. A generic `To[X, Y]` or `AsOptionT[X]`
dispatch layer would introduce an instance-coherence problem, complicate
error messages, and still need to resolve whether `Option[Option[A]]` means
wrapping the outer Option or lifting the whole receiver into a fresh effect.
There is no universal conversion dispatcher in this implementation.

The staged conditional builders have one deliberately narrow exception:
`Branch[F, Input, A]` interprets a branch argument as either an existing `F[A]`
or a pure A. It is sealed, does not introduce implicit conversions, and excludes
the pure instance for F-shaped inputs. Explicit `...F` and `...Pure` methods
avoid inference-dependent interpretation. This typeclass is not used by the
ordinary constructor extensions. See [conditional builders](conditionals.md).

Scala 3 uses native extension methods; Scala 2.13 uses scoped implicit-class
syntax with the same method names and behavior. The upstream datatype is still allocated
where its constructor requires it. No allocation-free or benchmark claim is
made. Public methods are not inline: that avoids deliberately distributing
implementation bodies to consumers and keeps ordinary binary compatibility
checks meaningful.

## Imports and dependency boundaries

The core, effect, and FS2 syntax modules are independent of one another. The
umbrella module depends on all three and provides the curated
`pspsps.syntax.common` and complete `pspsps.syntax.all` imports. Common includes
transformers, conditionals, basic Resource, Ref, Stream, Chunk, and nullable
interop. Specialized transformer, Pull, and advanced Resource syntax are opt-in
through focused imports or all. A downstream project may define its own aggregate by extending the
public syntax traits, or import a narrowly scoped object.

Do not use several wildcard imports for the *same* syntax family from
unrelated aggregate objects in the same scope. Import one aggregate or the
focused families you need. Compatibility with normal Cats and Cats Effect
syntax is exercised by compile-time contracts.

## Constraints are part of the API

Wrapping `F[Option[A]]`, `F[Either[E, A]]`, and `F[(L, A)]` adds no typeclass
constraint. Stream.eval and Resource.eval constructors also impose none.
Lifting OptionT/EitherT results requires Functor, while the chosen Cats
WriterT.liftF and IorT.liftF constructors require Applicative. Resource.make
requires Functor; Resource consumption imposes its own requirements later.

Some upstream constraints could be weakened by reimplementing constructors
in terms of lower-level operations. This library intentionally mirrors the
called public constructor instead. It does not invent a weaker guarantee
that upstream may not support.

## Overload escape hatches

OptionT/EitherT keep the short `asOptionT` and `asEitherT` spellings, with
`asOptionTWrap` and `asEitherTWrap` explicitly selecting wrapping. Cats already
provides `toOptionT` and `toEitherT` for lifting inner types, and `nested` for
Nested construction. pspsps does not duplicate those methods.

Constructors distinguish pure values, existing effects, stream elements, and
managed acquisitions through their names. A pure List is a value, a collection of
values, and a possible outer effect. A spelling should identify which
interpretation the caller selected.

## Suspension and lifecycle

Ordinary extension receivers are strict Scala expressions. The library does
not claim that an eager receiver, Future, or impure effect constructor becomes
lazy. It invokes no unsafe effect runner. For IO-based examples, IO delays
actual effects according to how the original IO was constructed.

All acquisition/release behavior comes from Cats Effect Resource. All stream
scope behavior comes from FS2. The library adds no custom finalizer stack,
background fiber, buffering, retry, or error-conversion policy.

The common import includes ordinary resource evaluation, ownership, and
finalization. `resourceInterop` covers acquisitions returning finalizers;
`resourceAdvanced` covers Poll-based acquisition. `resource` exposes the entire
family.

Resource.makeFull / makeCaseFull preserve Cats Effect's Poll contract: if the
acquisition is canceled before returning a value, its partial cleanup remains
the acquisition's responsibility. This is documented and tested explicitly.

## What is deliberately absent

Cats already supplies syntax such as `pure`, `some`, `asRight`, `mapN`,
`traverse`, and `toValidatedNec`. Duplicating those is not the purpose here.
Parameterless factories such as Deferred/Queue/Semaphore creation do not need
an artificial Unit receiver simply to look postfix. Iterator syntax is omitted
rather than silently sharing a consumed iterator across stream evaluations.
Unsafe runners and implicit conversions are not exposed.

Future candidates need a concrete motivating expression, unambiguous naming,
a constructor-coherence property, and relevant execution/inference tests.
Reasonable independent modules could cover Cats Free, HTTP integrations, or
additional FS2 constructors, but they should not enlarge core's dependencies.

## Staged conditionals

`Either.cond` and `Option.cond` build standard Scala values; their transformer
counterparts build Cats values. The exact companion-like spellings live in an
opt-in `pspsps.builders` namespace rather than trying to override existing Cats
or Scala members. Default syntax imports add postfix methods only.

Pure conditions evaluate once at entry. Branches remain by-name until selected
at completion; effectful conditions select inside F.flatMap. Neither unselected
branch factories nor their effects are started by the builder. This does not
suspend pre-existing eager effects or turn plain expressions into IO.delay.
True means Right or Some regardless of the order of supplied Either branches.
Option completes with Some because None has no payload to configure.

## Compatibility policy

This is a pre-1.0 API. The build adopts early-semver through sbt-typelevel.
Before the first release there is no published baseline against which MiMa can
prove compatibility. After release, preserve the upstream result types, method
names, receiver shapes, and constraints within the chosen compatibility line.

MiMa does not prove Scala source compatibility or overload inference.
The positive and negative compiler contracts are maintained for that reason.
The configured matrix is Scala 2.13 and Scala 3 on JVM and Scala.js. Execution
results are recorded in the repository's verification report.
