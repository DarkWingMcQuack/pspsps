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
import cats.data.{Cokleisli, Kleisli, Reader}

/** Constructors for effectful and context-dependent functions. */
trait KleisliSyntax:
  extension [F[_], R, A](run: R => F[A])
    /** Wrap an effectful function; the function is not invoked here. */
    def asKleisli: Kleisli[F, R, A] = Kleisli(run)

  extension [R, A](run: R => A)
    /** Wrap a pure environment-reading function. */
    def asReader: Reader[R, A] = Kleisli[cats.Id, R, A](run)

  extension [F[_], A, B](run: F[A] => B)
    /** Wrap a context-consuming function. */
    def asCokleisli: Cokleisli[F, A, B] = Cokleisli(run)

  extension [F[_], A](value: F[A])
    /** Ignore the environment and reuse the effect description. */
    def asKleisliLiftF[R]: Kleisli[F, R, A] = Kleisli.liftF(value)

  extension [A](value: A)
    /** Lift a constant value into an environment-independent Kleisli. */
    def asKleisliPure[F[_]: Applicative, R]: Kleisli[F, R, A] =
      Kleisli.pure[F, R, A](value)

object kleisli extends KleisliSyntax
