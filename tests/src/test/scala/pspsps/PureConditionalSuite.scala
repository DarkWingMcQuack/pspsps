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

import pspsps.builders.{Either, Option}
import pspsps.syntax.conditional._
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class PureConditionalSuite extends ScalaCheckSuite {
  property("Either agrees with the standard constructor in either branch order") {
    forAll { (test: Boolean, left: String, right: Int) =>
      val expected = scala.util.Either.cond(test, right, left)
      assertEquals(Either.cond(test).withLeft(left).withRight(right), expected)
      assertEquals(Either.cond(test).withRight(right).withLeft(left), expected)
    }
  }

  property("Option agrees with Option.when") {
    forAll { (test: Boolean, value: Int) =>
      assertEquals(Option.cond(test).withSome(value), scala.Option.when(test)(value))
    }
  }

  property("pure postfix entry points agree with the factory entry points") {
    forAll { (test: Boolean, value: Int) =>
      assertEquals(
        test.asEitherCond.withLeft("no").withRight(value),
        Either.cond(test).withLeft("no").withRight(value)
      )
      assertEquals(test.asOptionCond.withSome(value), Option.cond(test).withSome(value))
    }
  }

  test("false selects Left and true selects Right") {
    assertEquals(Either.cond(false).withLeft("no").withRight(42), Left("no"))
    assertEquals(Either.cond(true).withLeft("no").withRight(42), Right(42))
    assertEquals(Either.cond(false).withRight(42).withLeft("no"), Left("no"))
    assertEquals(Either.cond(true).withRight(42).withLeft("no"), Right(42))
  }

  test("pure branch factories run only at completion, and only on the selected side") {
    var events = List.empty[String]
    val stage = Either.cond(false).withLeft { events = events :+ "left"; "no" }
    assertEquals(events, Nil)
    val result = stage.withRight { events = events :+ "right"; 42 }
    assertEquals(result, Left("no"))
    assertEquals(events, List("left"))
  }

  test("right-first staging also delays the stored branch") {
    var calls = 0
    val stage = Either.cond(true).withRight { calls += 1; 42 }
    assertEquals(calls, 0)
    val result = stage.withLeft[String](throw new AssertionError("unselected left"))
    assertEquals(result, Right(42))
    assertEquals(calls, 1)
  }

  test("an unselected left is never evaluated") {
    val result = Either
      .cond(true)
      .withLeft[String](throw new AssertionError("left"))
      .withRight(42)
    assertEquals(result, Right(42))
  }

  test("an unselected right is never evaluated") {
    val result = Either
      .cond(false)
      .withLeft("no")
      .withRight[Int](throw new AssertionError("right"))
    assertEquals(result, Left("no"))
  }

  test("false Option does not evaluate its payload") {
    val result = Option.cond(false).withSome[Int](throw new AssertionError("payload"))
    assertEquals(result, None)
  }

  test("true Option evaluates its payload exactly once per completion") {
    var calls = 0
    val stage = Option.cond(true)
    assertEquals(calls, 0)
    assertEquals(stage.withSome { calls += 1; calls }, Some(1))
    assertEquals(stage.withSome { calls += 1; calls }, Some(2))
    assertEquals(calls, 2)
  }

  test("a reusable Either stage does not memoize a selected by-name branch") {
    var calls = 0
    val stage = Either.cond(false).withLeft { calls += 1; calls }
    assertEquals(stage.withRight("first"), Left(1))
    assertEquals(stage.withRight("second"), Left(2))
  }

  test("the pure condition is evaluated once at the entry point, not on completion") {
    var checks = 0
    val stage = Either.cond { checks += 1; true }.withLeft("no")
    assertEquals(checks, 1)
    assertEquals(stage.withRight(1), Right(1))
    assertEquals(stage.withRight(2), Right(2))
    assertEquals(checks, 1)
  }

  test("selected exceptions propagate instead of being converted into Left or None") {
    val boom = new IllegalStateException("selected")
    val eitherThrown = intercept[IllegalStateException] {
      Either.cond(true).withLeft("no").withRight[Int](throw boom)
    }
    val optionThrown = intercept[IllegalStateException] {
      Option.cond(true).withSome[Int](throw boom)
    }
    assert(eitherThrown eq boom)
    assert(optionThrown eq boom)
  }

  test("Some(null) is retained just as in Option.when") {
    val result = Option.cond(true).withSome[String](null)
    assertEquals(result, Some(null): scala.Option[String])
    assert(result.isDefined)
  }

  test("plain builders retain nested effects as values rather than interpreting them") {
    import cats.effect.IO
    val effect = IO.pure(42)
    val optional: scala.Option[IO[Int]] = Option.cond(true).withSome(effect)
    val either: scala.util.Either[String, IO[Int]] =
      Either.cond(true).withLeft("no").withRight(effect)
    assertEquals(optional, Some(effect))
    assertEquals(either, Right(effect))
  }

  test("results can immediately use existing transformer lift syntax") {
    import cats.effect.IO
    import pspsps.syntax.{eitherT, optionT}
    import eitherT._
    import optionT._
    val e: cats.data.EitherT[IO, String, Int] =
      Either.cond(true).withLeft("no").withRight(42).asEitherT[IO]
    val o: cats.data.OptionT[IO, Int] =
      Option.cond(true).withSome(42).asOptionT[IO]
    assert(e ne null)
    assert(o ne null)
  }
}
