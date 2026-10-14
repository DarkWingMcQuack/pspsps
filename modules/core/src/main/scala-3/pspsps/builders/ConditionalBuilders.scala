/*
 * Copyright 2026 pspsps contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pspsps.builders

import cats.{Applicative, FlatMap, Functor}
import cats.data.{EitherT as CatsEitherT, OptionT as CatsOptionT}
import scala.annotation.implicitNotFound
import scala.util.NotGiven

/** Internal-to-the-DSL interpretation of a branch argument.
  *
  * Automatic syntax unwraps exactly one F layer. The plain-value instance is unavailable for an
  * F-shaped input. Use withLeftPure / withRightPure / withSomePure when the effect itself is
  * intentionally the payload.
  *
  * Input is invariant: this prevents an effect from being widened to Any and silently selected as a
  * pure value. Widen effect subclasses to F[A], or use the explicit ...F methods (which supply that
  * expected type).
  */
@implicitNotFound(
  "Cannot interpret branch ${Input} in effect ${F} with payload ${A}. " +
    "Use an explicit withLeftF/withRightF/withSomeF or ...Pure method."
)
sealed trait Branch[F[_], Input, A]:
  def lift(input: => Input): F[A]

object Branch:
  // Id erases the outer type constructor, so generic effect inference needs this case.
  given identity[A]: Branch[cats.Id, A, A] with
    def lift(input: => A): A = input

  /** Contravariance recognizes effect subclasses without applying abstract F to a wildcard. */
  sealed trait EffectShape[F[_], -Input]

  object EffectShape:
    given applied[F[_], A]: EffectShape[F, F[A]] with {}

  given effect[F[_], A]: Branch[F, F[A], A] with
    def lift(input: => F[A]): F[A] = input

  given pure[F[_], A](using
      F: Applicative[F],
      notEffect: NotGiven[EffectShape[F, A]]
  ): Branch[F, A, A] with
    def lift(input: => A): F[A] = F.pure(input)

/** Chooses before either by-name branch is constructed. */
private[builders] sealed trait Decision[F[_]]:
  def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A]

private[builders] object Decision:
  def pure[F[_]](condition: Boolean): Decision[F] =
    new Decision[F]:
      def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A] =
        if condition then ifTrue else ifFalse

  def effect[F[_]](condition: F[Boolean])(using F: FlatMap[F]): Decision[F] =
    new Decision[F]:
      def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A] =
        F.flatMap(condition)(test => if test then ifTrue else ifFalse)

/** First stage: either branch can be supplied first; true always selects Right. */
final class EitherCondition[F[_]] private[builders] (decision: Decision[F]):
  def withLeft[Input, A](left: => Input)(using
      branch: Branch[F, Input, A]
  ): EitherWithLeft[F, A] =
    withLeftF(branch.lift(left))

  def withLeftF[A](left: => F[A]): EitherWithLeft[F, A] =
    new EitherWithLeft(decision, () => left)

  def withLeftPure[A](left: => A)(using F: Applicative[F]): EitherWithLeft[F, A] =
    withLeftF(F.pure(left))

  def withRight[Input, B](right: => Input)(using
      branch: Branch[F, Input, B]
  ): EitherWithRight[F, B] =
    withRightF(branch.lift(right))

  def withRightF[B](right: => F[B]): EitherWithRight[F, B] =
    new EitherWithRight(decision, () => right)

  def withRightPure[B](right: => B)(using F: Applicative[F]): EitherWithRight[F, B] =
    withRightF(F.pure(right))

/** Second stage after a left branch. Supplying Right returns Cats' EitherT. */
final class EitherWithLeft[F[_], A] private[builders] (
    decision: Decision[F],
    left: () => F[A]
):
  def withRight[Input, B](right: => Input)(using
      branch: Branch[F, Input, B],
      F: Functor[F]
  ): CatsEitherT[F, A, B] =
    withRightF(branch.lift(right))

  def withRightF[B](right: => F[B])(using F: Functor[F]): CatsEitherT[F, A, B] =
    CatsEitherT(
      decision.choose(
        F.map(right)(b => Right(b): scala.util.Either[A, B]),
        F.map(left())(a => Left(a): scala.util.Either[A, B])
      )
    )

  def withRightPure[B](right: => B)(using F: Applicative[F]): CatsEitherT[F, A, B] =
    withRightF(F.pure(right))

/** Second stage after a right branch. Supplying Left returns Cats' EitherT. */
final class EitherWithRight[F[_], B] private[builders] (
    decision: Decision[F],
    right: () => F[B]
):
  def withLeft[Input, A](left: => Input)(using
      branch: Branch[F, Input, A],
      F: Functor[F]
  ): CatsEitherT[F, A, B] =
    withLeftF(branch.lift(left))

  def withLeftF[A](left: => F[A])(using F: Functor[F]): CatsEitherT[F, A, B] =
    CatsEitherT(
      decision.choose(
        F.map(right())(b => Right(b): scala.util.Either[A, B]),
        F.map(left)(a => Left(a): scala.util.Either[A, B])
      )
    )

  def withLeftPure[A](left: => A)(using F: Applicative[F]): CatsEitherT[F, A, B] =
    withLeftF(F.pure(left))

/** true selects Some; false returns F.pure(None) without evaluating the payload. */
final class OptionCondition[F[_]] private[builders] (decision: Decision[F]):
  def withSome[Input, A](some: => Input)(using
      branch: Branch[F, Input, A],
      F: Applicative[F]
  ): CatsOptionT[F, A] =
    withSomeF(branch.lift(some))

  def withSomeF[A](some: => F[A])(using F: Applicative[F]): CatsOptionT[F, A] =
    CatsOptionT(
      decision.choose(
        F.map(some)(a => Some(a): scala.Option[A]),
        F.pure(scala.Option.empty[A])
      )
    )

  def withSomePure[A](some: => A)(using F: Applicative[F]): CatsOptionT[F, A] =
    withSomeF(F.pure(some))

/** A type alias, not a new transformer or an implicit conversion. */
type EitherT[F[_], A, B] = CatsEitherT[F, A, B]

/** Conditional-constructor namespace, intentionally not the Cats companion.
  *
  * Import pspsps.builders.EitherT for EitherT.cond(...). The original cats.data.EitherT.cond is
  * left untouched. This focused factory does not re-export Cats' other constructors: keep those
  * qualified or separately aliased.
  */
object EitherT:
  def cond[F[_]](condition: Boolean): EitherCondition[F] =
    condPure[F](condition)

  def cond[F[_]](condition: F[Boolean])(using FlatMap[F]): EitherCondition[F] =
    condF(condition)

  /** Explicit spelling, including when F = Id and the two input shapes overlap. */
  def condPure[F[_]](condition: Boolean): EitherCondition[F] =
    new EitherCondition(Decision.pure[F](condition))

  def condF[F[_]](condition: F[Boolean])(using FlatMap[F]): EitherCondition[F] =
    new EitherCondition(Decision.effect(condition))

/** A type alias, not a new transformer or an implicit conversion. */
type OptionT[F[_], A] = CatsOptionT[F, A]

/** Conditional-constructor namespace. False has no payload, so withSome completes it. */
object OptionT:
  def cond[F[_]](condition: Boolean): OptionCondition[F] =
    condPure[F](condition)

  def cond[F[_]](condition: F[Boolean])(using FlatMap[F]): OptionCondition[F] =
    condF(condition)

  def condPure[F[_]](condition: Boolean): OptionCondition[F] =
    new OptionCondition(Decision.pure[F](condition))

  def condF[F[_]](condition: F[Boolean])(using FlatMap[F]): OptionCondition[F] =
    new OptionCondition(Decision.effect(condition))
