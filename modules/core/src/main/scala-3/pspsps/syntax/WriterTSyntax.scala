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

import cats.{Applicative, Monoid}
import cats.data.{Writer, WriterT}

/** Postfix constructors for Writer and WriterT. */
trait WriterTSyntax:
  extension [F[_], L, A](value: F[(L, A)])
    /** Wrap an existing log/result effect, requiring no algebra for L or F. */
    def asWriterT: WriterT[F, L, A] = WriterT(value)

  extension [L, A](value: (L, A))
    /** Lift a log/result pair. */
    def asWriterTFromTuple[F[_]: Applicative]: WriterT[F, L, A] =
      WriterT(Applicative[F].pure(value))

    /** Construct a pure Writer from its log/result pair. */
    def asWriter: Writer[L, A] = WriterT[cats.Id, L, A](value)

  extension [F[_], A](value: F[A])
    /** Lift an effect, using the empty log. Matches WriterT.liftF constraints. */
    def asWriterTLiftF[L: Monoid](using Applicative[F]): WriterT[F, L, A] =
      WriterT.liftF[F, L, A](value)

  extension [A](value: A)
    /** Lift an already-computed value with an empty log. */
    def asWriterTValue[F[_]: Applicative, L: Monoid]: WriterT[F, L, A] =
      WriterT.value[F, L, A](value)

object writerT extends WriterTSyntax
