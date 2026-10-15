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

package pspsps

import cats.effect.{Deferred, IO, Ref}
import cats.effect.testkit.TestControl
import cats.instances.list._
import cats.syntax.all._
import pspsps.syntax.all._
import fs2.{Chunk, Stream}
import munit.CatsEffectSuite
import scala.concurrent.duration._

class StreamSuite extends CatsEffectSuite {
  test("eval is lazy and runs once per compilation of a singleton segment") {
    for {
      count <- Ref.of[IO, Int](0)
      stream = count.updateAndGet(_ + 1).asStreamEval
      before <- count.get
      first <- stream.compile.toList
      second <- stream.compile.toList
    } yield {
      assertEquals(before, 0)
      assertEquals(first, List(1))
      assertEquals(second, List(2))
    }
  }

  test("take(0) does not evaluate an effect") {
    for {
      count <- Ref.of[IO, Int](0)
      _ <- count.update(_ + 1).asStreamEval.take(0).compile.drain
      actual <- count.get
    } yield assertEquals(actual, 0)
  }

  test("eval failure remains stream failure") {
    val boom = new RuntimeException("stream")
    IO.raiseError[Int](boom).asStreamEval.compile.toList.attempt.map { result =>
      assertEquals(result, Left(boom))
    }
  }

  test("attemptEval emits an error as an element") {
    val boom = new RuntimeException("stream")
    IO.raiseError[Int](boom).asStreamAttemptEval.compile.toList.map { result =>
      assertEquals(result, List(Left(boom)))
    }
  }

  test("repeatEval re-evaluates only as far as downstream demands") {
    for {
      count <- Ref.of[IO, Int](0)
      values <- count.updateAndGet(_ + 1).asStreamRepeatEval.take(3).compile.toList
      actual <- count.get
    } yield {
      assertEquals(values, List(1, 2, 3))
      assertEquals(actual, 3)
    }
  }

  test("exec runs the action but emits nothing") {
    for {
      count <- Ref.of[IO, Int](0)
      values <- count.update(_ + 1).asStreamExec.covaryOutput[Unit].compile.toList
      actual <- count.get
    } yield {
      assertEquals(values, Nil)
      assertEquals(actual, 1)
    }
  }

  test("emit emits the collection itself") {
    List(1, 2).asStreamEmit[IO].compile.toList.map { result =>
      assertEquals(result, List(List(1, 2)))
    }
  }

  test("emits emits collection elements") {
    List(1, 2).asStreamEmits[IO].compile.toList.map { result =>
      assertEquals(result, List(1, 2))
    }
  }

  test("emits handles empty finite sequences") {
    List.empty[Int].asStreamEmits[IO].compile.toList.map(result => assertEquals(result, Nil))
  }

  test("eval emits one collection, evals emits its elements") {
    for {
      one <- IO.pure(List(1, 2)).asStreamEval.compile.toList
      many <- IO.pure(List(1, 2)).asStreamEvals.compile.toList
    } yield {
      assertEquals(one, List(List(1, 2)))
      assertEquals(many, List(1, 2))
    }
  }

  test("chunk preserves the provided chunk boundary") {
    Chunk(1, 2, 3).asStreamChunk[IO].chunks.compile.toList.map { result =>
      assertEquals(result.map(_.toList), List(List(1, 2, 3)))
    }
  }

  test("evalUnChunk preserves the effect-produced chunk boundary") {
    IO.pure(Chunk(1, 2, 3)).asStreamEvalUnChunk.chunks.compile.toList.map { result =>
      assertEquals(result.map(_.toList), List(List(1, 2, 3)))
    }
  }

  test("force runs the stream returned by the effect") {
    IO.pure(List(1, 2).asStreamEmits[IO]).asStreamEval.flatten.compile.toList.map { result =>
      assertEquals(result, List(1, 2))
    }
  }

  test("fromOption emits zero or one element") {
    for {
      some <- Option(42).asStreamFromOption[IO].compile.toList
      none <- Option.empty[Int].asStreamFromOption[IO].compile.toList
    } yield {
      assertEquals(some, List(42))
      assertEquals(none, Nil)
    }
  }

  test("fromOptionT evaluates only when demanded and again on each compilation") {
    for {
      count <- 0.asRef[IO]
      stream = count.updateAndGet(_ + 1).map(_.some).asOptionT.asStreamFromOptionT
      before <- count.get
      _ <- stream.take(0).compile.drain
      skipped <- count.get
      first <- stream.compile.toList
      second <- stream.compile.toList
    } yield {
      assertEquals(before, 0)
      assertEquals(skipped, 0)
      assertEquals(first, List(1))
      assertEquals(second, List(2))
    }
  }

  test("fromOptionT emits nothing for None") {
    none[Int].pure[IO].asOptionT.asStreamFromOptionT.compile.toList.map { result =>
      assertEquals(result, Nil)
    }
  }

  test("fromOptionT preserves outer effect failures") {
    val boom = new RuntimeException("outer")
    IO.raiseError[Option[Int]](boom).asOptionT.asStreamFromOptionT.compile.toList.attempt.map {
      result => assertEquals(result, boom.asLeft[List[Int]])
    }
  }

  test("fromEither emits Right as a singleton") {
    42.asRight[RuntimeException].asStreamFromEither[IO].compile.toList.map { result =>
      assertEquals(result, List(42))
    }
  }

  test("fromEither fails with the Throwable in Left") {
    val boom = new IllegalArgumentException("left")
    boom.asLeft[Int].asStreamFromEither[IO].compile.toList.attempt.map { result =>
      assertEquals(result, boom.asLeft[List[Int]])
    }
  }

  test("fromEitherT evaluates only when demanded and again on each compilation") {
    for {
      count <- 0.asRef[IO]
      stream = count
        .updateAndGet(_ + 1)
        .map(_.asRight[IllegalArgumentException])
        .asEitherT
        .asStreamFromEitherT
      before <- count.get
      _ <- stream.take(0).compile.drain
      skipped <- count.get
      first <- stream.compile.toList
      second <- stream.compile.toList
    } yield {
      assertEquals(before, 0)
      assertEquals(skipped, 0)
      assertEquals(first, List(1))
      assertEquals(second, List(2))
    }
  }

  test("fromEitherT fails with the Throwable in Left") {
    val boom = new IllegalArgumentException("left")
    boom.asLeft[Int].pure[IO].asEitherT.asStreamFromEitherT.compile.toList.attempt.map { result =>
      assertEquals(result, boom.asLeft[List[Int]])
    }
  }

  test("fromEitherT preserves outer effect failures") {
    val boom = new RuntimeException("outer")
    IO.raiseError[Either[IllegalArgumentException, Int]](boom)
      .asEitherT
      .asStreamFromEitherT
      .compile
      .toList
      .attempt
      .map { result => assertEquals(result, boom.asLeft[List[Int]]) }
  }

  test("resource releases after successful stream use") {
    for {
      released <- Ref.of[IO, Int](0)
      values <- IO
        .pure(42)
        .asResourceMake(_ => released.update(_ + 1))
        .asStreamResource
        .compile
        .toList
      count <- released.get
    } yield {
      assertEquals(values, List(42))
      assertEquals(count, 1)
    }
  }

  test("resource releases on downstream failure") {
    val boom = new RuntimeException("downstream")
    for {
      released <- Ref.of[IO, Int](0)
      result <- IO
        .pure(42)
        .asResourceMake(_ => released.update(_ + 1))
        .asStreamResource
        .evalMap(_ => IO.raiseError[Unit](boom))
        .compile
        .drain
        .attempt
      count <- released.get
    } yield {
      assertEquals(result, Left(boom))
      assertEquals(count, 1)
    }
  }

  test("resource releases on early termination") {
    for {
      released <- Ref.of[IO, Int](0)
      values <- IO
        .pure(42)
        .asResourceMake(_ => released.update(_ + 1))
        .asStreamResource
        .flatMap(n => Stream.constant(n).covary[IO])
        .take(2)
        .compile
        .toList
      count <- released.get
    } yield {
      assertEquals(values, List(42, 42))
      assertEquals(count, 1)
    }
  }

  test("resource releases on stream cancellation") {
    for {
      released <- Ref.of[IO, Int](0)
      entered <- Deferred[IO, Unit]
      fiber <- IO.unit
        .asResourceMake(_ => released.update(_ + 1))
        .asStreamResource
        .flatMap(_ => (entered.complete(()).void *> IO.never[Unit]).asStreamExec)
        .compile
        .drain
        .start
      _ <- entered.get
      _ <- fiber.cancel
      count <- released.get
    } yield assertEquals(count, 1)
  }

  test("take(0) never acquires a resource") {
    for {
      acquired <- Ref.of[IO, Int](0)
      _ <- acquired
        .update(_ + 1)
        .asResourceMake(_ => IO.unit)
        .asStreamResource
        .take(0)
        .compile
        .drain
      actual <- acquired.get
    } yield assertEquals(actual, 0)
  }

  test("sleep is tested with virtual time") {
    TestControl.executeEmbed(10.millis.asStreamSleep[IO].compile.toList).map { result =>
      assertEquals(result, List(()))
    }
  }

  test("awakeEvery is tested with virtual time") {
    TestControl.executeEmbed(10.millis.asStreamAwakeEvery[IO].take(3).compile.toList).map {
      result =>
        assertEquals(result, List(10.millis, 20.millis, 30.millis))
    }
  }
}
