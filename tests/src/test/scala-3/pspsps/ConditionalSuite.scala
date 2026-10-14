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

import cats.{Applicative, FlatMap, Functor, Id}
import cats.data.{EitherT as CatsEitherT, OptionT as CatsOptionT}
import cats.effect.{Deferred, IO, Ref}
import cats.instances.list.*
import cats.syntax.all.*
import pspsps.builders.{EitherT, OptionT}
import pspsps.syntax.conditional.*
import munit.CatsEffectSuite

/** Semantic tests: expected to be executed on both JVM and Scala.js by sbt verify. */
class ConditionalSuite extends CatsEffectSuite:
  private def eitherMatrix(flag: Boolean): List[EitherT[IO, String, Int]] =
    List(
      EitherT.cond[IO](flag).withLeft("no").withRight(42),
      EitherT.cond[IO](flag).withLeft(IO.pure("no")).withRight(42),
      EitherT.cond[IO](flag).withLeft("no").withRight(IO.pure(42)),
      EitherT.cond[IO](flag).withLeft(IO.pure("no")).withRight(IO.pure(42)),
      EitherT.cond(IO.pure(flag)).withLeft("no").withRight(42),
      EitherT.cond(IO.pure(flag)).withLeft(IO.pure("no")).withRight(42),
      EitherT.cond(IO.pure(flag)).withLeft("no").withRight(IO.pure(42)),
      EitherT.cond(IO.pure(flag)).withLeft(IO.pure("no")).withRight(IO.pure(42)),
      EitherT.cond[IO](flag).withRight(42).withLeft("no"),
      EitherT.cond[IO](flag).withRight(42).withLeft(IO.pure("no")),
      EitherT.cond[IO](flag).withRight(IO.pure(42)).withLeft("no"),
      EitherT.cond[IO](flag).withRight(IO.pure(42)).withLeft(IO.pure("no")),
      EitherT.cond(IO.pure(flag)).withRight(42).withLeft("no"),
      EitherT.cond(IO.pure(flag)).withRight(42).withLeft(IO.pure("no")),
      EitherT.cond(IO.pure(flag)).withRight(IO.pure(42)).withLeft("no"),
      EitherT.cond(IO.pure(flag)).withRight(IO.pure(42)).withLeft(IO.pure("no"))
    )

  test("EitherT accepts all eight pure/effect combinations in either branch order: true") {
    eitherMatrix(true).traverse(_.value).map { results =>
      assertEquals(results, List.fill(16)(Right(42): Either[String, Int]))
    }
  }

  test("EitherT accepts all eight pure/effect combinations in either branch order: false") {
    eitherMatrix(false).traverse(_.value).map { results =>
      assertEquals(results, List.fill(16)(Left("no"): Either[String, Int]))
    }
  }

  private def optionMatrix(flag: Boolean): List[OptionT[IO, Int]] =
    List(
      OptionT.cond[IO](flag).withSome(42),
      OptionT.cond[IO](flag).withSome(IO.pure(42)),
      OptionT.cond(IO.pure(flag)).withSome(42),
      OptionT.cond(IO.pure(flag)).withSome(IO.pure(42))
    )

  test("OptionT accepts all four pure/effect combinations: true") {
    optionMatrix(true).traverse(_.value).map { results =>
      assertEquals(results, List.fill(4)(Option(42)))
    }
  }

  test("OptionT accepts all four pure/effect combinations: false") {
    optionMatrix(false).traverse(_.value).map { results =>
      assertEquals(results, List.fill(4)(Option.empty[Int]))
    }
  }

  test("effectful condition is run before, and only before, the selected IO branch") {
    for
      events <- Ref.of[IO, List[String]](Nil)
      result = EitherT
        .cond(events.update(_ :+ "condition").as(true))
        .withLeft(events.update(_ :+ "left").as("no"))
        .withRight(events.update(_ :+ "right").as(42))
      before <- events.get
      value <- result.value
      after <- events.get
    yield
      assertEquals(before, Nil)
      assertEquals(value, Right(42))
      assertEquals(after, List("condition", "right"))
  }

  test("false runs only the left effect") {
    for
      events <- Ref.of[IO, List[String]](Nil)
      result <- EitherT
        .cond(events.update(_ :+ "condition").as(false))
        .withLeft(events.update(_ :+ "left").as("no"))
        .withRight(events.update(_ :+ "right").as(42))
        .value
      seen <- events.get
    yield
      assertEquals(result, Left("no"))
      assertEquals(seen, List("condition", "left"))
  }

  test("an unselected pure left expression is not evaluated") {
    EitherT
      .cond(IO.pure(true))
      .withLeftPure[String](throw new AssertionError("unselected left"))
      .withRight(42)
      .value
      .map(result => assertEquals(result, Right(42)))
  }

  test("an unselected pure right expression is not evaluated") {
    EitherT
      .cond(IO.pure(false))
      .withRightPure[Int](throw new AssertionError("unselected right"))
      .withLeft("no")
      .value
      .map(result => assertEquals(result, Left("no")))
  }

  test("an unselected effect factory is not called") {
    def forbidden: IO[String] = throw new AssertionError("factory called")
    EitherT
      .cond(IO.pure(true))
      .withLeft(forbidden)
      .withRight(42)
      .value
      .map(result => assertEquals(result, Right(42)))
  }

  test("pure conditions also skip construction of unselected branches") {
    def forbidden: IO[Int] = throw new AssertionError("factory called")
    EitherT
      .cond[IO](false)
      .withLeft("no")
      .withRight(forbidden)
      .value
      .map(result => assertEquals(result, Left("no")))
  }

  test("OptionT false skips both pure expressions and effect factories") {
    def forbidden: IO[Int] = throw new AssertionError("factory called")
    val a = OptionT.cond(IO.pure(false)).withSome(forbidden)
    val b = OptionT.cond[IO](false).withSomePure[Int](throw new AssertionError("payload"))
    (a.value, b.value).tupled.map { result =>
      assertEquals(result, (Option.empty[Int], Option.empty[Int]))
    }
  }

  test("OptionT false does not run the supplied effect") {
    for
      calls <- Ref.of[IO, Int](0)
      result <- OptionT.cond(IO.pure(false)).withSome(calls.updateAndGet(_ + 1)).value
      count <- calls.get
    yield
      assertEquals(result, None)
      assertEquals(count, 0)
  }

  test("condition failure remains an outer failure and skips both branches") {
    val boom = new RuntimeException("condition")
    for
      calls <- Ref.of[IO, Int](0)
      result <- EitherT
        .cond(IO.raiseError[Boolean](boom))
        .withLeft(calls.update(_ + 1).as("no"))
        .withRight(calls.update(_ + 1).as(42))
        .value
        .attempt
      count <- calls.get
    yield
      assertEquals(result, Left(boom))
      assertEquals(count, 0)
  }

  test("a failed selected right effect is not a domain Left") {
    val boom = new RuntimeException("right")
    EitherT
      .cond(IO.pure(true))
      .withLeft("no")
      .withRight(IO.raiseError[Int](boom))
      .value
      .attempt
      .map(result => assertEquals(result, Left(boom)))
  }

  test("a failed selected left effect is not a domain Left") {
    val boom = new RuntimeException("left")
    EitherT
      .cond(IO.pure(false))
      .withLeft(IO.raiseError[String](boom))
      .withRight(42)
      .value
      .attempt
      .map(result => assertEquals(result, Left(boom)))
  }

  test("a failed OptionT payload is not None") {
    val boom = new RuntimeException("payload")
    OptionT
      .cond(IO.pure(true))
      .withSome(IO.raiseError[Int](boom))
      .value
      .attempt
      .map(result => assertEquals(result, Left(boom)))
  }

  test("OptionT condition failure skips the payload") {
    val boom = new RuntimeException("condition")
    def forbidden: IO[Int] = throw new AssertionError("payload factory")
    OptionT
      .cond(IO.raiseError[Boolean](boom))
      .withSome(forbidden)
      .value
      .attempt
      .map(result => assertEquals(result, Left(boom)))
  }

  test("rerunning an IO value reevaluates the condition and selected branch, without memoization") {
    for
      checks <- Ref.of[IO, Int](0)
      lefts <- Ref.of[IO, Int](0)
      rights <- Ref.of[IO, Int](0)
      result = EitherT
        .cond(checks.updateAndGet(_ + 1).map(_ % 2 == 1))
        .withLeft(lefts.updateAndGet(_ + 1))
        .withRight(rights.updateAndGet(_ + 1))
      first <- result.value
      second <- result.value
      c <- checks.get
      l <- lefts.get
      r <- rights.get
    yield
      assertEquals(first, Right(1))
      assertEquals(second, Left(1))
      assertEquals((c, l, r), (2, 1, 1))
  }

  test("canceling an IO condition does not start either branch") {
    for
      started <- Deferred[IO, Unit]
      calls <- Ref.of[IO, Int](0)
      result = EitherT
        .cond(started.complete(()).void *> IO.never[Boolean])
        .withLeft(calls.update(_ + 1).as("no"))
        .withRight(calls.update(_ + 1).as(42))
      fiber <- result.value.start
      _ <- started.get
      _ <- fiber.cancel
      outcome <- fiber.join
      count <- calls.get
    yield
      assert(outcome.isCanceled)
      assertEquals(count, 0)
  }

  test("postfix and explicit condition spellings agree") {
    val variants: List[EitherT[IO, String, Int]] = List(
      IO.pure(true).asEitherTCond.withLeft("no").withRight(42),
      IO.pure(true).asEitherTCondF.withLeftF(IO.pure("no")).withRightPure(42),
      true.asEitherTCond[IO].withRight(42).withLeft("no"),
      true.asEitherTCondPure[IO].withRightF(IO.pure(42)).withLeftPure("no")
    )
    variants
      .traverse(_.value)
      .map(values => assertEquals(values, List.fill(4)(Right(42): Either[String, Int])))
  }

  test("OptionT postfix and explicit condition spellings agree") {
    val variants: List[OptionT[IO, Int]] = List(
      IO.pure(true).asOptionTCond.withSome(42),
      IO.pure(true).asOptionTCondF.withSomeF(IO.pure(42)),
      true.asOptionTCond[IO].withSomePure(42),
      true.asOptionTCondPure[IO].withSome(42)
    )
    variants.traverse(_.value).map(values => assertEquals(values, List.fill(4)(Option(42))))
  }

  test("explicit Pure methods retain an effect as payload instead of running it") {
    for
      calls <- Ref.of[IO, Int](0)
      payload = calls.updateAndGet(_ + 1)
      result: CatsOptionT[IO, IO[Int]] = OptionT.cond(IO.pure(true)).withSomePure(payload)
      some <- result.value
      before <- calls.get
      value <- some.getOrElse(IO.raiseError(new AssertionError("expected Some")))
      after <- calls.get
    yield
      assertEquals(before, 0)
      assertEquals(value, 1)
      assertEquals(after, 1)
  }

  test("automatic branch lifting removes exactly one effect layer") {
    val result: CatsEitherT[IO, String, IO[Int]] =
      EitherT.cond(IO.pure(true)).withLeft("no").withRight(IO.pure(IO.pure(42)))
    result.value.flatMap {
      case Right(inner) => inner.map(value => assertEquals(value, 42))
      case Left(error)  => IO.raiseError(new AssertionError(error))
    }
  }

  test("Id is supported using explicit condition disambiguators") {
    given cats.Monad[Id] = cats.catsInstancesForId
    val either: CatsEitherT[Id, String, Int] =
      EitherT.condPure[Id](true).withLeft("no").withRight(42)
    val option: CatsOptionT[Id, Int] = OptionT.condF[Id](false).withSome(42)
    assertEquals(either.value, Right(42))
    assertEquals(option.value, None)
  }

  test("List conditions preserve flatMap multiplicity and branch order") {
    val either = EitherT
      .cond(List(true, false, true))
      .withLeft(List("a", "b"))
      .withRight(List(1, 2))
    val option = OptionT.cond(List(true, false, true)).withSome(List(1, 2))
    assertEquals(either.value, List(Right(1), Right(2), Left("a"), Left("b"), Right(1), Right(2)))
    assertEquals(option.value, List(Some(1), Some(2), None, Some(1), Some(2)))
  }

  test("empty outer effects are not confused with Left or inner None") {
    import cats.instances.option.*
    val condition: Option[Boolean] = None
    val either = EitherT.cond(condition).withLeft("no").withRight(42)
    val option = OptionT.cond(condition).withSome(42)
    assertEquals(either.value, None)
    assertEquals(option.value, None)
  }

  test("a builder can be reused; adding the second branch does not consume the first stage") {
    val partial = EitherT.cond[IO](true).withLeft("no")
    (partial.withRight(1).value, partial.withRight(2).value).tupled.map { values =>
      assertEquals(values, (Right(1), Right(2)))
    }
  }

  test("pure condition with Id selects lazily at completion, not at the first stage") {
    given cats.Monad[Id] = cats.catsInstancesForId
    var calls = 0
    val step = EitherT.condPure[Id](false).withLeftPure { calls += 1; "no" }
    assertEquals(calls, 0)
    val result = step.withRightPure(42)
    assertEquals(calls, 1)
    assertEquals(result.value, Left("no"))
  }

  test("explicit F methods accept effect subclasses with a supplied expected type") {
    import cats.instances.option.*
    val result: CatsOptionT[Option, Int] = OptionT.condPure[Option](true).withSomeF(Some(42))
    assertEquals(result.value, Some(Some(42)))
  }

  test("minimal explicit APIs agree with concrete IO results") {
    val variants = List(
      pureEither[IO](true),
      effectBranches[IO](true, IO.pure("no"), IO.pure(42)),
      effectCondition[IO](IO.pure(true), IO.pure("no"), IO.pure(42))
    )
    variants
      .traverse(_.value)
      .map(values => assertEquals(values, List.fill(3)(Right(42): Either[String, Int])))
  }

  test("a different context is a plain payload, not the chosen outer effect") {
    val result: CatsOptionT[IO, List[Int]] =
      OptionT.cond[IO](true).withSome(List(1, 2))
    result.value.map(value => assertEquals(value, Some(List(1, 2))))
  }

  // These declarations are compile contracts for the minimal explicit APIs.
  private def pureEither[F[_]: Applicative](flag: Boolean): CatsEitherT[F, String, Int] =
    EitherT.condPure[F](flag).withLeftPure("no").withRightPure(42)

  private def effectBranches[F[_]: Functor](
      flag: Boolean,
      left: F[String],
      right: F[Int]
  ): CatsEitherT[F, String, Int] =
    EitherT.condPure[F](flag).withLeftF(left).withRightF(right)

  private def effectCondition[F[_]: FlatMap](
      flag: F[Boolean],
      left: F[String],
      right: F[Int]
  ): CatsEitherT[F, String, Int] =
    EitherT.condF(flag).withLeftF(left).withRightF(right)
