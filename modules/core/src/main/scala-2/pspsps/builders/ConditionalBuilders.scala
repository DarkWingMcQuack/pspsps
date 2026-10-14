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

package pspsps

package object builders {
  type EitherT[F[_], A, B] = cats.data.EitherT[F, A, B]
  type OptionT[F[_], A] = cats.data.OptionT[F, A]
  type Either[+A, +B] = scala.util.Either[A, B]
  type Option[+A] = scala.Option[A]
}

package builders {
  import cats.{Applicative, FlatMap, Functor}
  import cats.data.{EitherT => CatsEitherT, OptionT => CatsOptionT}
  import scala.annotation.{implicitNotFound, unused}

  /** Interprets one branch argument, removing exactly one layer of the chosen effect. Input remains
    * invariant so an effect cannot silently widen into a pure payload.
    */
  @implicitNotFound(
    "Cannot interpret branch ${Input} in effect ${F} with payload ${A}. " +
      "Use an explicit withLeftF/withRightF/withSomeF or ...Pure method."
  )
  sealed trait Branch[F[_], Input, A] {
    def lift(input: => Input): F[A]
  }

  object Branch {
    implicit def identity[A]: Branch[cats.Id, A, A] = new Branch[cats.Id, A, A] {
      def lift(input: => A): A = input
    }

    sealed trait EffectShape[F[_], -Input]

    object EffectShape {
      implicit def applied[F[_], A]: EffectShape[F, F[A]] = new EffectShape[F, F[A]] {}
    }

    /** Scala 2 negative evidence: an available value makes the two witnesses ambiguous. */
    sealed trait Absent[A]

    object Absent {
      implicit def absent[A]: Absent[A] = new Absent[A] {}
      implicit def ambiguous1[A](implicit @unused evidence: A): Absent[A] = new Absent[A] {}
      implicit def ambiguous2[A](implicit @unused evidence: A): Absent[A] = new Absent[A] {}
    }

    implicit def effect[F[_], A]: Branch[F, F[A], A] = new Branch[F, F[A], A] {
      def lift(input: => F[A]): F[A] = input
    }

    implicit def pure[F[_], A](implicit
        F: Applicative[F],
        @unused notEffect: Absent[EffectShape[F, A]]
    ): Branch[F, A, A] = new Branch[F, A, A] {
      def lift(input: => A): F[A] = F.pure(input)
    }
  }

  private[builders] sealed trait Decision[F[_]] {
    def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A]
  }

  private[builders] object Decision {
    def pure[F[_]](condition: Boolean): Decision[F] = new Decision[F] {
      def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A] =
        if (condition) ifTrue else ifFalse
    }

    def effect[F[_]](condition: F[Boolean])(implicit F: FlatMap[F]): Decision[F] =
      new Decision[F] {
        def choose[A](ifTrue: => F[A], ifFalse: => F[A]): F[A] =
          F.flatMap(condition)(test => if (test) ifTrue else ifFalse)
      }
  }

  /** Either branch can be supplied first; true always selects Right. */
  final class EitherCondition[F[_]] private[builders] (decision: Decision[F]) {
    def withLeft[Input, A](left: => Input)(implicit
        branch: Branch[F, Input, A]
    ): EitherWithLeft[F, A] = withLeftF(branch.lift(left))

    def withLeftF[A](left: => F[A]): EitherWithLeft[F, A] =
      new EitherWithLeft(decision, () => left)

    def withLeftPure[A](left: => A)(implicit F: Applicative[F]): EitherWithLeft[F, A] =
      withLeftF(F.pure(left))

    def withRight[Input, B](right: => Input)(implicit
        branch: Branch[F, Input, B]
    ): EitherWithRight[F, B] = withRightF(branch.lift(right))

    def withRightF[B](right: => F[B]): EitherWithRight[F, B] =
      new EitherWithRight(decision, () => right)

    def withRightPure[B](right: => B)(implicit F: Applicative[F]): EitherWithRight[F, B] =
      withRightF(F.pure(right))
  }

  final class EitherWithLeft[F[_], A] private[builders] (
      decision: Decision[F],
      left: () => F[A]
  ) {
    def withRight[Input, B](right: => Input)(implicit
        branch: Branch[F, Input, B],
        F: Functor[F]
    ): CatsEitherT[F, A, B] = withRightF(branch.lift(right))

    def withRightF[B](right: => F[B])(implicit F: Functor[F]): CatsEitherT[F, A, B] =
      CatsEitherT(
        decision.choose(
          F.map(right)(b => Right(b): scala.util.Either[A, B]),
          F.map(left())(a => Left(a): scala.util.Either[A, B])
        )
      )

    def withRightPure[B](right: => B)(implicit F: Applicative[F]): CatsEitherT[F, A, B] =
      withRightF(F.pure(right))
  }

  final class EitherWithRight[F[_], B] private[builders] (
      decision: Decision[F],
      right: () => F[B]
  ) {
    def withLeft[Input, A](left: => Input)(implicit
        branch: Branch[F, Input, A],
        F: Functor[F]
    ): CatsEitherT[F, A, B] = withLeftF(branch.lift(left))

    def withLeftF[A](left: => F[A])(implicit F: Functor[F]): CatsEitherT[F, A, B] =
      CatsEitherT(
        decision.choose(
          F.map(right())(b => Right(b): scala.util.Either[A, B]),
          F.map(left)(a => Left(a): scala.util.Either[A, B])
        )
      )

    def withLeftPure[A](left: => A)(implicit F: Applicative[F]): CatsEitherT[F, A, B] =
      withLeftF(F.pure(left))
  }

  /** False returns an inner None without constructing the by-name payload. */
  final class OptionCondition[F[_]] private[builders] (decision: Decision[F]) {
    def withSome[Input, A](some: => Input)(implicit
        branch: Branch[F, Input, A],
        F: Applicative[F]
    ): CatsOptionT[F, A] = withSomeF(branch.lift(some))

    def withSomeF[A](some: => F[A])(implicit F: Applicative[F]): CatsOptionT[F, A] =
      CatsOptionT(
        decision.choose(
          F.map(some)(a => Some(a): scala.Option[A]),
          F.pure(scala.Option.empty[A])
        )
      )

    def withSomePure[A](some: => A)(implicit F: Applicative[F]): CatsOptionT[F, A] =
      withSomeF(F.pure(some))
  }

  /** Opt-in conditional factory; the result is Cats' existing EitherT. */
  object EitherT {
    def cond[F[_]](condition: Boolean): EitherCondition[F] = condPure[F](condition)

    def cond[F[_]](condition: F[Boolean])(implicit F: FlatMap[F]): EitherCondition[F] =
      condF(condition)

    def condPure[F[_]](condition: Boolean): EitherCondition[F] =
      new EitherCondition(Decision.pure[F](condition))

    def condF[F[_]](condition: F[Boolean])(implicit F: FlatMap[F]): EitherCondition[F] =
      new EitherCondition(Decision.effect(condition))
  }

  /** Opt-in conditional factory; the result is Cats' existing OptionT. */
  object OptionT {
    def cond[F[_]](condition: Boolean): OptionCondition[F] = condPure[F](condition)

    def cond[F[_]](condition: F[Boolean])(implicit F: FlatMap[F]): OptionCondition[F] =
      condF(condition)

    def condPure[F[_]](condition: Boolean): OptionCondition[F] =
      new OptionCondition(Decision.pure[F](condition))

    def condF[F[_]](condition: F[Boolean])(implicit F: FlatMap[F]): OptionCondition[F] =
      new OptionCondition(Decision.effect(condition))
  }
}
