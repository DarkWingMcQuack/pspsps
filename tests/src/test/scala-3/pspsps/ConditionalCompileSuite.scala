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

import munit.FunSuite
import scala.compiletime.testing.typeChecks

class ConditionalCompileSuite extends FunSuite:
  test("effectful entry infers F and both branch result types without a result annotation") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      val inferred = EitherT.cond(IO.pure(true)).withLeft(IO.pure("no")).withRight(IO.pure(42))
      val checked: cats.data.EitherT[IO, String, Int] = inferred
    """))
  }

  test("mixed and reversed branch inference is independent of an expected result type") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      val a = EitherT.cond(IO.pure(true)).withLeft("no").withRight(IO.pure(42))
      val b = EitherT.cond[IO](true).withRight(42).withLeft(IO.pure("no"))
      val c = EitherT.cond[IO](true).withLeft("no").withRight(42)
      val checked: List[cats.data.EitherT[IO, String, Int]] = List(a, b, c)
    """))
  }

  test("OptionT infers pure and effectful payloads without a result annotation") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.builders.OptionT
      val a = OptionT.cond(IO.pure(true)).withSome(IO.pure(42))
      val b = OptionT.cond[IO](true).withSome(42)
      val checked: List[cats.data.OptionT[IO, Int]] = List(a, b)
    """))
  }

  test("automatic F branches do not silently become pure payloads of type Any") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.OptionT
      val result = OptionT.cond[IO](true).withSome[IO[Int], Any](IO.pure(42))
    """))
  }

  test("the completed transformer has the inferred channel types") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      val result = EitherT.cond(IO.pure(true)).withLeft("no").withRight(42)
      val wrong: cats.data.EitherT[IO, Int, String] = result
    """))
  }

  test("an incomplete builder is not an EitherT") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      EitherT.cond[IO](true).withLeft("no").value
    """))
  }

  test("the left stage does not allow a second left branch") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      EitherT.cond[IO](true).withLeft("no").withLeft("again")
    """))
  }

  test("explicit effect branches reject a different outer effect") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      EitherT.cond(IO.pure(true)).withLeftF(Option("no")).withRight(42)
    """))
  }

  test("non-Boolean conditions are rejected") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.EitherT
      EitherT.cond(IO.pure(1))
    """))
  }

  test("automatic effect-subclass arguments must be widened or use the explicit F method") {
    assert(!typeChecks("""
      import cats.instances.option.*
      import pspsps.builders.OptionT
      OptionT.condPure[Option](true).withSome(Some(42))
    """))
  }

  test("family imports include their own conditional syntax") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.eitherT.*
      val result: cats.data.EitherT[IO, String, Int] =
        IO.pure(true).asEitherTCond.withLeft("no").withRight(42)
    """))
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.optionT.*
      val result: cats.data.OptionT[IO, Int] =
        IO.pure(true).asOptionTCond.withSome(42)
    """))
  }

  test("umbrella and Cats syntax imports coexist without replacing Cats companions") {
    assert(typeChecks("""
      import cats.effect.IO
      import cats.data.{EitherT, OptionT}
      import cats.syntax.all.*
      import pspsps.syntax.all.*
      val oldEither = EitherT.cond[IO](true, 42, "no")
      val oldOption = OptionT.when[IO, Int](true)(42)
      val result: EitherT[IO, String, Int] =
        IO.pure(true).asEitherTCond.withLeft("no").withRight(42)
    """))
  }
