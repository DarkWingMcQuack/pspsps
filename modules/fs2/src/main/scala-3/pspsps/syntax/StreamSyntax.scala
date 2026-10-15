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

import cats.Foldable
import cats.data.{EitherT, OptionT}
import cats.effect.kernel.{MonadCancel, Resource, Temporal}
import fs2.{Chunk, RaiseThrowable, Stream}
import scala.concurrent.duration.FiniteDuration

/** Postfix FS2 constructors with no extra execution, buffering, or scopes. */
trait StreamSyntax:
  extension [F[_], A](value: F[A])
    /** Evaluate when pulled and emit one result. Does not execute F here. */
    def asStreamEval: Stream[F, A] = Stream.eval(value)

    /** Evaluate when pulled, emitting an Either for success or failure. */
    def asStreamAttemptEval: Stream[F, Either[Throwable, A]] =
      Stream.attemptEval(value)

    /** Re-evaluate the effect as downstream demand requests more elements. */
    def asStreamRepeatEval: Stream[F, A] = Stream.repeatEval(value)

  extension [F[_]](value: F[Unit])
    /** Run the action when pulled but emit no elements. */
    def asStreamExec: Stream[F, Nothing] = Stream.exec(value)

  extension [A](value: A)
    /** Emit the value itself, including when it is a collection or effect. */
    def asStreamEmit[F[_]]: Stream[F, A] = Stream.emit(value).covary[F]

  extension [A](values: scala.collection.Seq[A])
    /** Emit the elements of a finite sequence. Does not promise lazy traversal. */
    def asStreamEmits[F[_]]: Stream[F, A] = Stream.emits(values).covary[F]

  extension [A](values: Chunk[A])
    /** Emit an existing chunk, retaining its chunk boundary. */
    def asStreamChunk[F[_]]: Stream[F, A] = Stream.chunk(values).covary[F]

  extension [F[_], A](value: F[Chunk[A]])
    /** Evaluate an effect and emit its chunk. */
    def asStreamEvalUnChunk: Stream[F, A] = Stream.evalUnChunk(value)

  extension [F[_], G[_], A](value: F[G[A]])
    /** Evaluate once, then emit a finite Foldable's elements. This follows Stream.evals, which
      * materializes the Foldable as a List.
      */
    def asStreamEvals(using Foldable[G]): Stream[F, A] = Stream.evals(value)

  extension [F[_], A](value: Resource[F, A])
    /** Use the Resource within FS2's resource scope, retaining finalization. */
    def asStreamResource(using MonadCancel[F, ?]): Stream[F, A] =
      Stream.resource(value)

  extension [A](value: Option[A])
    /** Emit zero or one element according to the Option. */
    def asStreamFromOption[F[_]]: Stream[F, A] = Stream.fromOption[F](value)

  extension [F[_], A](value: OptionT[F, A])
    /** Evaluate when pulled, emitting Some's value or nothing for None. */
    def asStreamFromOptionT: Stream[F, A] = Stream.eval(value.value).unNone

  extension [E <: Throwable, A](value: Either[E, A])
    /** Emit Right's value or fail with the Throwable in Left. */
    def asStreamFromEither[F[_]: RaiseThrowable]: Stream[F, A] = Stream.fromEither[F](value)

  extension [F[_], E <: Throwable, A](value: EitherT[F, E, A])
    /** Evaluate when pulled, emitting Right's value or failing with Left's Throwable. */
    def asStreamFromEitherT(using RaiseThrowable[F]): Stream[F, A] =
      Stream.eval(value.value).rethrow

  extension (duration: FiniteDuration)
    /** Sleep, then emit Unit. */
    def asStreamSleep[F[_]: Temporal]: Stream[F, Unit] = Stream.sleep[F](duration)

    /** Periodically emit elapsed time using FS2's awakeEvery semantics. */
    def asStreamAwakeEvery[F[_]: Temporal]: Stream[F, FiniteDuration] =
      Stream.awakeEvery[F](duration)

object stream extends StreamSyntax
