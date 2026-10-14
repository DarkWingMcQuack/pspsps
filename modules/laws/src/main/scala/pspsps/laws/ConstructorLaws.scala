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

import cats.Applicative
import cats.data.{EitherT, OptionT}
import cats.laws.IsEq
import pspsps.syntax.core._

/** Coherence with upstream constructors, not a new effect algebra. */
final class ConstructorLaws[F[_]](implicit F: Applicative[F]) {
  def optionWrap[A](value: F[Option[A]]): IsEq[F[Option[A]]] =
    IsEq(value.asOptionTWrap.value, value)

  def optionFromOption[A](value: Option[A]): IsEq[F[Option[A]]] =
    IsEq(value.asOptionT[F].value, F.pure(value))

  def optionLiftF[A](value: F[A]): IsEq[F[Option[A]]] =
    IsEq(value.asOptionTLiftF.value, OptionT.liftF(value).value)

  def optionPure[A](value: A): IsEq[F[Option[A]]] =
    IsEq(value.asOptionTPure[F].value, OptionT.pure[F](value).value)

  def optionMap[A, B](value: F[Option[A]], f: A => B): IsEq[F[Option[B]]] =
    IsEq(value.asOptionTWrap.map(f).value, F.map(value)(_.map(f)))

  def eitherWrap[E, A](value: F[Either[E, A]]): IsEq[F[Either[E, A]]] =
    IsEq(value.asEitherTWrap.value, value)

  def eitherFromEither[E, A](value: Either[E, A]): IsEq[F[Either[E, A]]] =
    IsEq(value.asEitherT[F].value, F.pure(value))

  def eitherLiftF[E, A](value: F[A]): IsEq[F[Either[E, A]]] =
    IsEq(value.asEitherTLiftF[E].value, EitherT.liftF[F, E, A](value).value)

  def eitherPure[E, A](value: A): IsEq[F[Either[E, A]]] =
    IsEq(value.asEitherTPure[F, E].value, EitherT.pure[F, E](value).value)

  def eitherMap[E, A, B](value: F[Either[E, A]], f: A => B): IsEq[F[Either[E, B]]] =
    IsEq(value.asEitherTWrap.map(f).value, F.map(value)(_.map(f)))

}

object ConstructorLaws {
  def apply[F[_]: Applicative]: ConstructorLaws[F] = new ConstructorLaws[F]
}
