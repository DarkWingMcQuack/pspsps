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

import cats.Id
import cats.effect.{IO, Ref}
import cats.implicits._
import pspsps.builders.{Either => EitherBuilder, EitherT, Option => OptionBuilder, OptionT}
import munit.CatsEffectSuite

class ConditionalSuite extends CatsEffectSuite {
  test("automatic branches infer pure, effectful, mixed and reversed payloads") {
    val pure = EitherT.cond[IO](true).withLeft("no").withRight(42)
    val effects = EitherT.cond(IO.pure(true)).withLeft(IO.pure("no")).withRight(IO.pure(42))
    val leftEffect = EitherT.cond[IO](true).withLeft(IO.pure("no")).withRight(42)
    val rightEffect = EitherT.cond(IO.pure(true)).withLeft("no").withRight(IO.pure(42))
    val reversed = EitherT.cond(IO.pure(false)).withRight(IO.pure(42)).withLeft("no")
    val inferred: List[EitherT[IO, String, Int]] =
      List(pure, effects, leftEffect, rightEffect, reversed)

    inferred.traverse(_.value).map { results =>
      assertEquals(results, List(Right(42), Right(42), Right(42), Right(42), Left("no")))
    }
  }

  test("OptionT infers both branch interpretations and removes one effect layer") {
    val pure = OptionT.cond[IO](true).withSome(42)
    val effect = OptionT.cond(IO.pure(true)).withSome(IO.pure(42))
    val inferred: List[OptionT[IO, Int]] = List(pure, effect)
    val nested: OptionT[IO, IO[Int]] =
      OptionT.cond(IO.pure(true)).withSome(IO.pure(IO.pure(42)))

    for {
      values <- inferred.traverse(_.value)
      inner <- nested.value
      result <- inner.getOrElse(IO.raiseError(new AssertionError("expected Some")))
    } yield {
      assertEquals(values, List(Some(42), Some(42)))
      assertEquals(result, 42)
    }
  }

  test("effectful selection delays and sequences only the selected branch") {
    for {
      events <- Ref.of[IO, List[String]](Nil)
      result = EitherT
        .cond(events.update(_ :+ "condition").as(false))
        .withRight(events.update(_ :+ "right").as(42))
        .withLeft(events.update(_ :+ "left").as("no"))
      before <- events.get
      value <- result.value
      after <- events.get
    } yield {
      assertEquals(before, Nil)
      assertEquals(value, Left("no"))
      assertEquals(after, List("condition", "left"))
    }
  }

  test("unselected factories are skipped for pure and effectful conditions") {
    def forbidden: IO[Int] = throw new AssertionError("unselected factory")
    val either = EitherT.condPure[IO](false).withLeftPure("no").withRightF(forbidden)
    val option = OptionT.condF(IO.pure(false)).withSomeF(forbidden)

    (either.value, option.value).tupled.map { result =>
      assertEquals(result, (Left("no"), None))
    }
  }

  test("a failed condition remains an outer failure and never constructs a branch") {
    val boom = new RuntimeException("condition")
    def forbidden: IO[Int] = throw new AssertionError("branch factory")
    EitherT
      .condF(IO.raiseError[Boolean](boom))
      .withLeftF(forbidden)
      .withRightF(forbidden)
      .value
      .attempt
      .map { result =>
        assertEquals(result, Left(boom))
      }
  }

  test("List conditions retain flatMap multiplicity and order") {
    val either = EitherT
      .cond(List(true, false, true))
      .withRight(List(1, 2))
      .withLeft(List("a", "b"))
    val option = OptionT.cond(List(true, false, true)).withSome(List(1, 2))

    assertEquals(either.value, List(Right(1), Right(2), Left("a"), Left("b"), Right(1), Right(2)))
    assertEquals(option.value, List(Some(1), Some(2), None, Some(1), Some(2)))
  }

  test("an empty outer condition is distinct from an inner None") {
    val condition: Option[Boolean] = None
    val optional: OptionT[Option, Int] = OptionT.cond(condition).withSome(42)
    val either: EitherT[Option, String, Int] = EitherT.cond(condition).withLeft("no").withRight(42)
    assertEquals(optional.value, None)
    assertEquals(either.value, None)
  }

  test("explicit Pure keeps an effect as payload without executing it") {
    for {
      calls <- Ref.of[IO, Int](0)
      payload = calls.updateAndGet(_ + 1)
      result = OptionT.condPure[IO](true).withSomePure(payload)
      optional <- result.value
      before <- calls.get
      value <- optional.getOrElse(IO.raiseError(new AssertionError("expected Some")))
      after <- calls.get
    } yield {
      assertEquals(before, 0)
      assertEquals(value, 1)
      assertEquals(after, 1)
    }
  }

  test("Id uses explicit condition spellings without changing payload inference") {
    val either: EitherT[Id, String, Int] =
      EitherT.condPure[Id](true).withLeft("no").withRight(42)
    val option: OptionT[Id, Int] = OptionT.condF[Id](false).withSome(42)
    assertEquals(either.value, Right(42))
    assertEquals(option.value, None)
  }

  test("plain Either stages delay construction and do not memoize the selected branch") {
    var calls = 0
    val stage = EitherBuilder.cond(false).withLeft { calls += 1; calls }
    assertEquals(calls, 0)
    assertEquals(stage.withRight[String](throw new AssertionError("unselected")), Left(1))
    assertEquals(stage.withRight("unused"), Left(2))
  }

  test("plain Option skips a false payload") {
    assertEquals(
      OptionBuilder.cond(false).withSome[Int](throw new AssertionError("unselected")),
      None
    )
  }

  test("plain Option retains Some(null)") {
    assertEquals(OptionBuilder.cond(true).withSome[String](null), Some(null): Option[String])
  }

  test("automatic effect subclasses require widening or the explicit F method") {
    assert(compileErrors("""
      import cats.instances.option._
      import pspsps.builders.OptionT
      OptionT.condPure[Option](true).withSome(Some(42))
    """).nonEmpty)
    val explicit: OptionT[Option, Int] = OptionT.condPure[Option](true).withSomeF(Some(42))
    assertEquals(explicit.value, Some(Some(42)))
  }

  test("an effect cannot silently widen into a pure Any payload") {
    assert(compileErrors("""
      import cats.effect.IO
      import pspsps.builders.OptionT
      OptionT.condPure[IO](true).withSome[IO[Int], Any](IO.pure(42))
    """).nonEmpty)
  }

  test("incomplete stages do not expose the completed transformer") {
    assert(compileErrors("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      EitherT.condPure[IO](true).withLeftPure("no").value
    """).nonEmpty)
  }

  test("builder namespaces expose no unrelated companion constructors") {
    assert(compileErrors("""
      pspsps.builders.Option(42)
    """).nonEmpty)
    assert(compileErrors("""
      pspsps.builders.Either.cond(true, 42, "no")
    """).nonEmpty)
  }
}
