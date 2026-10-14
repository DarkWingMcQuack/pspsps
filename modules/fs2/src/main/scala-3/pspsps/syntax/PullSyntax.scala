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

import fs2.{Chunk, Pull}

/** Pull distinguishes emitted output from its terminal result. */
trait PullSyntax:
  extension [F[_], A](value: F[A])
    /** Evaluate to a terminal result; emit no output. */
    def asPullEval: Pull[F, Nothing, A] = Pull.eval(value)

    /** Evaluate to an Either terminal result; emit no output. */
    def asPullAttemptEval: Pull[F, Nothing, Either[Throwable, A]] =
      Pull.attemptEval(value)

  extension [A](value: A)
    /** Finish with the value as a result; do not emit it. */
    def asPullPure[F[_]]: Pull[F, Nothing, A] = Pull.pure[F, A](value)

    /** Emit the value and finish with Unit. */
    def asPullOutput1[F[_]]: Pull[F, A, Unit] = Pull.output1[F, A](value)

  extension [A](values: Chunk[A])
    /** Emit a whole chunk and finish with Unit. */
    def asPullOutput[F[_]]: Pull[F, A, Unit] = Pull.output[F, A](values)

object pull extends PullSyntax
