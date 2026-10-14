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

package pspsps.laws

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.laws.IsEq
import pspsps.builders.{EitherT => FluentEitherT, OptionT => FluentOptionT}

/** Conditional builders preserve the outer effect's branching and failure semantics. */
final class ConditionalLaws[F[_]](implicit F: Monad[F]) {
  def eitherCondition[A, B](condition: F[Boolean], left: F[A], right: F[B]): IsEq[F[Either[A, B]]] =
    IsEq(
      FluentEitherT.condF(condition).withLeftF(left).withRightF(right).value,
      F.ifM(condition)(EitherT.right[A](right).value, EitherT.left[B](left).value)
    )

  def eitherBranchOrder[A, B](
      condition: F[Boolean],
      left: F[A],
      right: F[B]
  ): IsEq[F[Either[A, B]]] =
    IsEq(
      FluentEitherT.condF(condition).withRightF(right).withLeftF(left).value,
      FluentEitherT.condF(condition).withLeftF(left).withRightF(right).value
    )

  def eitherComplement[A, B](
      condition: F[Boolean],
      left: F[A],
      right: F[B]
  ): IsEq[F[Either[A, B]]] =
    IsEq(
      F.map(
        FluentEitherT.condF(F.map(condition)(!_)).withLeftF(right).withRightF(left).value
      )(_.swap),
      FluentEitherT.condF(condition).withLeftF(left).withRightF(right).value
    )

  def optionCondition[A](condition: F[Boolean], some: F[A]): IsEq[F[Option[A]]] =
    IsEq(
      FluentOptionT.condF(condition).withSomeF(some).value,
      F.ifM(condition)(OptionT.liftF(some).value, OptionT.none[F, A].value)
    )

}

object ConditionalLaws {
  def apply[F[_]: Monad]: ConditionalLaws[F] = new ConditionalLaws[F]
}
