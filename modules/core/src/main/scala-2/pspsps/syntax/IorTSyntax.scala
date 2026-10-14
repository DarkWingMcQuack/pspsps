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

import cats.Applicative
import cats.data.{Ior, IorT}

/** Postfix constructors for inclusive-or transformers. */
trait IorTSyntax {
  implicit final class IorTWrapOps[F[_], E, A](private val value: F[Ior[E, A]]) {

    /** Wrap an existing F[Ior[E, A]], preserving Left, Right, and Both. */
    def asIorT: IorT[F, E, A] = IorT(value)
  }

  implicit final class IorTFromIorOps[E, A](private val value: Ior[E, A]) {

    /** Lift a plain Ior, preserving all three cases. */
    def asIorTFromIor[F[_]: Applicative]: IorT[F, E, A] =
      IorT.fromIor[F](value)
  }

  implicit final class IorTEffectOps[F[_], A](private val value: F[A]) {

    /** Match the Applicative constraint of Cats' IorT.liftF constructor. */
    def asIorTLiftF[E](implicit F: Applicative[F]): IorT[F, E, A] =
      IorT.liftF[F, E, A](value)
  }

  implicit final class IorTValueOps[A](private val value: A) {

    /** Lift an already-computed value to the right side. */
    def asIorTPure[F[_]: Applicative, E]: IorT[F, E, A] =
      IorT.pure[F, E](value)
  }
}

object iorT extends IorTSyntax
