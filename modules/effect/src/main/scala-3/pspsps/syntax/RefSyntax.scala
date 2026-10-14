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

import cats.FlatMap
import cats.effect.kernel.Ref

/** Effectful Ref allocation; no unsafe constructor is exposed. */
trait RefSyntax:
  extension [A](initial: A)
    /** Allocate a fresh Ref each time the returned F is executed. */
    def asRef[F[_]: Ref.Make]: F[Ref[F, A]] = Ref.of[F, A](initial)

  extension [F[_], A](initial: F[A])
    /** Evaluate an initial value, then allocate a fresh Ref. */
    def asRefOfEffect(using Ref.Make[F], FlatMap[F]): F[Ref[F, A]] =
      Ref.ofEffect(initial)

object ref extends RefSyntax
