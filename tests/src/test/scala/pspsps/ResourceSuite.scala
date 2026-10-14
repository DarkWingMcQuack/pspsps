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
import cats.effect.kernel.{Poll, Resource}
import pspsps.syntax.all._
import munit.CatsEffectSuite

class ResourceSuite extends CatsEffectSuite {
  test("asResourceEval is lazy and re-evaluated on every use") {
    for {
      count <- Ref.of[IO, Int](0)
      resource = count.updateAndGet(_ + 1).asResourceEval
      before <- count.get
      first <- resource.use(IO.pure)
      second <- resource.use(IO.pure)
    } yield {
      assertEquals(before, 0)
      assertEquals(first, 1)
      assertEquals(second, 2)
    }
  }

  test("asResourcePure returns an already-computed value") {
    42.asResourcePure[IO].use(IO.pure).map(value => assertEquals(value, 42))
  }

  test("make releases exactly once after success") {
    for {
      released <- Ref.of[IO, Int](0)
      result <- IO.pure(42).asResourceMake(_ => released.update(_ + 1)).use(IO.pure)
      count <- released.get
    } yield {
      assertEquals(result, 42)
      assertEquals(count, 1)
    }
  }

  test("make releases exactly once after use failure") {
    val boom = new RuntimeException("use")
    for {
      released <- Ref.of[IO, Int](0)
      result <- IO.unit
        .asResourceMake(_ => released.update(_ + 1))
        .use(_ => IO.raiseError[Unit](boom))
        .attempt
      count <- released.get
    } yield {
      assertEquals(result, Left(boom))
      assertEquals(count, 1)
    }
  }

  test("make releases after cancellation, with no sleep-based race") {
    for {
      released <- Ref.of[IO, Int](0)
      entered <- Deferred[IO, Unit]
      fiber <- IO.unit
        .asResourceMake(_ => released.update(_ + 1))
        .use(_ => entered.complete(()).void *> IO.never[Unit])
        .start
      _ <- entered.get
      _ <- fiber.cancel
      count <- released.get
    } yield assertEquals(count, 1)
  }

  test("failed acquisition does not register a finalizer for an unacquired value") {
    val boom = new RuntimeException("acquire")
    for {
      released <- Ref.of[IO, Int](0)
      result <- IO
        .raiseError[Int](boom)
        .asResourceMake(_ => released.update(_ + 1))
        .use(IO.pure)
        .attempt
      count <- released.get
    } yield {
      assertEquals(result, Left(boom))
      assertEquals(count, 0)
    }
  }

  test("makeCase reports success") {
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      _ <- IO.unit.asResourceMakeCase((_, ec) => exit.set(Some(ec))).use(IO.pure)
      actual <- exit.get
    } yield assertEquals(actual, Some(Resource.ExitCase.Succeeded))
  }

  test("makeCase reports failure") {
    val boom = new RuntimeException("use")
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      _ <- IO.unit
        .asResourceMakeCase((_, ec) => exit.set(Some(ec)))
        .use(_ => IO.raiseError[Unit](boom))
        .attempt
      actual <- exit.get
    } yield assertEquals(actual, Some(Resource.ExitCase.Errored(boom)))
  }

  test("makeCase reports cancellation") {
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      entered <- Deferred[IO, Unit]
      fiber <- IO.unit
        .asResourceMakeCase((_, ec) => exit.set(Some(ec)))
        .use(_ => entered.complete(()).void *> IO.never[Unit])
        .start
      _ <- entered.get
      _ <- fiber.cancel
      actual <- exit.get
    } yield assertEquals(actual, Some(Resource.ExitCase.Canceled))
  }

  test("nested resources release in reverse acquisition order") {
    for {
      events <- Ref.of[IO, List[String]](Nil)
      first = IO.pure(1).asResourceMake(_ => events.update(_ :+ "first"))
      second = IO.pure(2).asResourceMake(_ => events.update(_ :+ "second"))
      _ <- first.flatMap(_ => second).use(IO.pure)
      actual <- events.get
    } yield assertEquals(actual, List("second", "first"))
  }

  test("resourceWrap keeps the finalizer returned by acquisition") {
    for {
      released <- Ref.of[IO, Int](0)
      result <- IO.pure((42, released.update(_ + 1))).asResourceWrap.use(IO.pure)
      count <- released.get
    } yield {
      assertEquals(result, 42)
      assertEquals(count, 1)
    }
  }

  test("resourceWrapCase keeps its exit-case-aware finalizer") {
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      release = (ec: Resource.ExitCase) => exit.set(Some(ec))
      _ <- IO.pure((42, release)).asResourceWrapCase.use(IO.pure)
      actual <- exit.get
    } yield assertEquals(actual, Some(Resource.ExitCase.Succeeded))
  }

  test("suspend retains the lifecycle of the resource returned by the effect") {
    for {
      released <- Ref.of[IO, Int](0)
      inner = IO.pure(42).asResourceMake(_ => released.update(_ + 1))
      value <- IO.pure(inner).asResourceEval.flatMap(identity).use(IO.pure)
      count <- released.get
    } yield {
      assertEquals(value, 42)
      assertEquals(count, 1)
    }
  }

  test("makeFull allows a waiting acquire to be canceled and clean up partial work") {
    for {
      entered <- Deferred[IO, Unit]
      partialCleanup <- Ref.of[IO, Int](0)
      released <- Ref.of[IO, Int](0)
      acquire = (poll: Poll[IO]) =>
        (entered.complete(()).void *> poll(IO.never[Int]))
          .onCancel(partialCleanup.update(_ + 1))
      fiber <- acquire.asResourceMakeFull(_ => released.update(_ + 1)).use(IO.pure).start
      _ <- entered.get
      _ <- fiber.cancel
      cleaned <- partialCleanup.get
      count <- released.get
    } yield {
      assertEquals(cleaned, 1)
      assertEquals(count, 0)
    }
  }

  test("makeCaseFull keeps the release exit case") {
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      acquire = (poll: Poll[IO]) => poll(IO.pure(42))
      value <- acquire.asResourceMakeCaseFull((_, ec) => exit.set(Some(ec))).use(IO.pure)
      actual <- exit.get
    } yield {
      assertEquals(value, 42)
      assertEquals(actual, Some(Resource.ExitCase.Succeeded))
    }
  }

  test("onFinalize registers cleanup without acquisition") {
    for {
      released <- Ref.of[IO, Int](0)
      _ <- released.update(_ + 1).asResourceOnFinalize.use(IO.pure)
      count <- released.get
    } yield assertEquals(count, 1)
  }

  test("onFinalizeCase registers exit-case-aware cleanup") {
    for {
      exit <- Ref.of[IO, Option[Resource.ExitCase]](None)
      release = (ec: Resource.ExitCase) => exit.set(Some(ec))
      _ <- release.asResourceOnFinalizeCase.use(IO.pure)
      actual <- exit.get
    } yield assertEquals(actual, Some(Resource.ExitCase.Succeeded))
  }

  test("a failing finalizer is not swallowed") {
    val boom = new RuntimeException("release")
    IO.unit.asResourceMake(_ => IO.raiseError[Unit](boom)).use(IO.pure).attempt.map { result =>
      assertEquals(result, Left(boom))
    }
  }

  test("AutoCloseable release is installed") {
    val closed = new RuntimeException("closed")
    val acquire = IO.delay(
      new AutoCloseable {
        def close(): Unit = throw closed
      }
    )
    acquire.asResourceAutoCloseable.use(_ => IO.unit).attempt.map { result =>
      assertEquals(result, Left(closed))
    }
  }

  test("eval does not silently close an AutoCloseable") {
    val closed = new RuntimeException("must not close")
    val acquire = IO.delay(
      new AutoCloseable {
        def close(): Unit = throw closed
      }
    )
    acquire.asResourceEval.use(_ => IO.unit).attempt.map { result =>
      assertEquals(result, Right(()))
    }
  }
}
