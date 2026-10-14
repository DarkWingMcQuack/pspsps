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
import cats.data.{IndexedStateT, State, StateT}

/** Postfix constructors for state transitions. */
trait StateTSyntax:
  extension [F[_], S, A](run: S => F[(S, A)])
    /** Build a state transition without invoking it. */
    def asStateT(using Applicative[F]): StateT[F, S, A] = StateT(run)

  extension [F[_], S1, S2, A](run: S1 => F[(S2, A)])
    /** Build an indexed transition whose input and output state types may differ. */
    def asIndexedStateT(using Applicative[F]): IndexedStateT[F, S1, S2, A] =
      IndexedStateT(run)

  extension [F[_], S, A](run: F[S => F[(S, A)]])
    /** Wrap an already-effectful transition description without constraints. */
    def asStateTApplyF: StateT[F, S, A] = StateT.applyF(run)

  extension [S, A](run: S => (S, A))
    /** Build a pure Cats State, whose underlying effect is Eval. */
    def asState: State[S, A] = State(run)

  extension [F[_], A](value: F[A])
    /** Lift an effect while leaving the state unchanged. */
    def asStateTLiftF[S](using Applicative[F]): StateT[F, S, A] =
      StateT.liftF[F, S, A](value)

  extension [A](value: A)
    /** Lift an already-computed result while leaving the state unchanged. */
    def asStateTPure[F[_]: Applicative, S]: StateT[F, S, A] =
      StateT.pure[F, S, A](value)

object stateT extends StateTSyntax
