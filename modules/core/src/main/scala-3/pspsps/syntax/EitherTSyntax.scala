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

package pspsps.syntax

import cats.{Applicative, Functor}
import cats.data.EitherT

/** Postfix constructors for EitherT, with explicit error-channel semantics. */
trait EitherTSyntax extends EitherTConditionalSyntax:
  extension [F[_], E, A](value: F[Either[E, A]])
    /** Wrap the existing nested effect. Does not catch outer-effect errors. */
    def asEitherT: EitherT[F, E, A] = EitherT(value)

    /** Unambiguous wrapping when the outer effect is itself an Either. */
    def asEitherTWrap: EitherT[F, E, A] = EitherT(value)

  extension [E, A](value: Either[E, A])
    /** Lift a plain Either into an explicitly selected outer effect. */
    def asEitherT[F[_]: Applicative]: EitherT[F, E, A] =
      EitherT.fromEither[F](value)

  extension [F[_], A](value: F[A])
    /** Lift successful results to Right; does NOT turn failures of F into Left. */
    def asEitherTLiftF[E](using Functor[F]): EitherT[F, E, A] =
      EitherT.liftF[F, E, A](value)

    /** Lift successful results of F into the Left channel. */
    def asEitherTLeftF[B](using Functor[F]): EitherT[F, A, B] =
      EitherT.left[B](value)

  extension [A](value: A)
    /** Lift an already-computed successful value. */
    def asEitherTPure[F[_]: Applicative, E]: EitherT[F, E, A] =
      EitherT.pure[F, E](value)

    /** Lift an already-computed domain error. */
    def asEitherTLeftT[F[_]: Applicative, B]: EitherT[F, A, B] =
      EitherT.leftT[F, B](value)

object eitherT extends EitherTSyntax
