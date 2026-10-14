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
import cats.data.OptionT

/** Postfix constructors for OptionT. No operation invokes an effect runner. */
trait OptionTSyntax extends OptionTConditionalSyntax:
  extension [F[_], A](value: F[Option[A]])
    /** Wrap the existing outer effect. For overlapping shapes, use asOptionTWrap. */
    def asOptionT: OptionT[F, A] = OptionT(value)

    /** Unambiguous spelling of OptionT(value), even for Option[Option[A]]. */
    def asOptionTWrap: OptionT[F, A] = OptionT(value)

  extension [A](value: Option[A])
    /** Lift a plain Option into the explicitly selected outer effect. */
    def asOptionT[F[_]: Applicative]: OptionT[F, A] =
      OptionT.fromOption[F](value)

  extension [F[_], A](value: F[A])
    /** Wrap successful results in Some. Failures in F remain failures in F. */
    def asOptionTLiftF(using Functor[F]): OptionT[F, A] =
      OptionT.liftF(value)

  extension [A](value: A)
    /** Lift an already-computed value. This is not a suspension boundary. */
    def asOptionTPure[F[_]: Applicative]: OptionT[F, A] =
      OptionT.pure[F](value)

object optionT extends OptionTSyntax
