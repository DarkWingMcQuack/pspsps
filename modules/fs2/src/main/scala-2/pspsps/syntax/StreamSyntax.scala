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
import cats.effect.kernel.{MonadCancel, Resource, Temporal}
import fs2.{Chunk, Stream}
import scala.concurrent.duration.FiniteDuration

/** Postfix FS2 constructors with no extra execution, buffering, or scopes. */
trait StreamSyntax {
  implicit final class StreamEffectOps[F[_], A](private val value: F[A]) {

    /** Evaluate when pulled and emit one result. Does not execute F here. */
    def asStreamEval: Stream[F, A] = Stream.eval(value)

    /** Evaluate when pulled, emitting an Either for success or failure. */
    def asStreamAttemptEval: Stream[F, Either[Throwable, A]] =
      Stream.attemptEval(value)

    /** Re-evaluate the effect as downstream demand requests more elements. */
    def asStreamRepeatEval: Stream[F, A] = Stream.repeatEval(value)
  }

  implicit final class StreamExecOps[F[_]](private val value: F[Unit]) {

    /** Run the action when pulled but emit no elements. */
    def asStreamExec: Stream[F, Nothing] = Stream.exec(value)
  }

  implicit final class StreamValueOps[A](private val value: A) {

    /** Emit the value itself, including when it is a collection or effect. */
    def asStreamEmit[F[_]]: Stream[F, A] = Stream.emit(value).covary[F]
  }

  implicit final class StreamSequenceOps[A](private val values: scala.collection.Seq[A]) {

    /** Emit the elements of a finite sequence. Does not promise lazy traversal. */
    def asStreamEmits[F[_]]: Stream[F, A] = Stream.emits(values).covary[F]
  }

  implicit final class StreamChunkOps[A](private val values: Chunk[A]) {

    /** Emit an existing chunk, retaining its chunk boundary. */
    def asStreamChunk[F[_]]: Stream[F, A] = Stream.chunk(values).covary[F]
  }

  implicit final class StreamEvalChunkOps[F[_], A](private val value: F[Chunk[A]]) {

    /** Evaluate an effect and emit its chunk. */
    def asStreamEvalUnChunk: Stream[F, A] = Stream.evalUnChunk(value)
  }

  implicit final class StreamEvalsOps[F[_], G[_], A](private val value: F[G[A]]) {

    /** Evaluate once, then emit a finite Foldable's elements. This follows Stream.evals, which
      * materializes the Foldable as a List.
      */
    def asStreamEvals(implicit G: Foldable[G]): Stream[F, A] = Stream.evals(value)
  }

  implicit final class StreamResourceOps[F[_], A](private val value: Resource[F, A]) {

    /** Use the Resource within FS2's resource scope, retaining finalization. */
    def asStreamResource(implicit F: MonadCancel[F, _]): Stream[F, A] =
      Stream.resource(value)
  }

  implicit final class StreamOptionOps[A](private val value: Option[A]) {

    /** Emit zero or one element according to the Option. */
    def asStreamFromOption[F[_]]: Stream[F, A] = Stream.fromOption[F](value)
  }

  implicit final class StreamDurationOps(private val duration: FiniteDuration) {

    /** Sleep, then emit Unit. */
    def asStreamSleep[F[_]: Temporal]: Stream[F, Unit] = Stream.sleep[F](duration)

    /** Periodically emit elapsed time using FS2's awakeEvery semantics. */
    def asStreamAwakeEvery[F[_]: Temporal]: Stream[F, FiniteDuration] =
      Stream.awakeEvery[F](duration)
  }
}

object stream extends StreamSyntax
