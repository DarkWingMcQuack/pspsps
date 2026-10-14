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

class PureConditionalCompileSuite extends FunSuite:
  test("plain constructors infer Scala result types without an expected type") {
    assert(typeChecks("""
      import pspsps.builders.{Either, Option}
      val e = Either.cond(true).withLeft("no").withRight(42)
      val r = Either.cond(true).withRight(42).withLeft("no")
      val o = Option.cond(true).withSome(42)
      val checkedE: scala.util.Either[String, Int] = e
      val checkedR: scala.util.Either[String, Int] = r
      val checkedO: scala.Option[Int] = o
    """))
  }

  test("all four factory namespaces can be imported together") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.builders.{Either, EitherT, Option, OptionT}
      val e: Either[String, Int] = Either.cond(true).withLeft("no").withRight(42)
      val o: Option[Int] = Option.cond(true).withSome(42)
      val et: EitherT[IO, String, Int] =
        EitherT.cond(IO.pure(true)).withLeft("no").withRight(IO.pure(42))
      val ot: OptionT[IO, Int] = OptionT.cond[IO](true).withSome(42)
    """))
  }

  test("focused plain imports work independently") {
    assert(typeChecks("""
      import pspsps.syntax.either.*
      val result: Either[String, Int] = true.asEitherCond.withLeft("no").withRight(42)
    """))
    assert(typeChecks("""
      import pspsps.syntax.option.*
      val result: Option[Int] = true.asOptionCond.withSome(42)
    """))
  }

  test("core and umbrella imports expose plain conditionals without replacing companions") {
    assert(typeChecks("""
      import pspsps.syntax.core.*
      val a = true.asEitherCond.withLeft("no").withRight(42)
      val b = true.asOptionCond.withSome(42)
      val standardE = Either.cond(true, 42, "no")
      val standardO = Option.when(true)(42)
    """))
    assert(typeChecks("""
      import cats.syntax.all.*
      import pspsps.syntax.all.*
      val a = true.asEitherCond.withLeft("no").withRight(42)
      val b = true.asOptionCond.withSome(42)
    """))
  }

  test("plain constructors reject effectful conditions instead of running an effect") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.Either
      Either.cond(IO.pure(true)).withLeft("no").withRight(42)
    """))
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.builders.Option
      Option.cond(IO.pure(true)).withSome(42)
    """))
  }

  test("non-Boolean pure conditions are rejected") {
    assert(!typeChecks("""
      import pspsps.builders.Option
      Option.cond(1).withSome(42)
    """))
  }

  test("an incomplete pure builder is not an Either") {
    assert(!typeChecks("""
      import pspsps.builders.Either
      val result: scala.util.Either[String, Int] = Either.cond(true).withLeft("no")
    """))
  }

  test("a second left branch is not allowed") {
    assert(!typeChecks("""
      import pspsps.builders.Either
      Either.cond(true).withLeft("no").withLeft("again")
    """))
  }

  test("a second right branch is not allowed") {
    assert(!typeChecks("""
      import pspsps.builders.Either
      Either.cond(true).withRight(42).withRight(43)
    """))
  }

  test("plain payload effects are retained in the inferred result type") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.builders.{Either, Option}
      val e = Either.cond(true).withLeft("no").withRight(IO.pure(42))
      val o = Option.cond(true).withSome(IO.pure(42))
      val checkedE: scala.util.Either[String, IO[Int]] = e
      val checkedO: scala.Option[IO[Int]] = o
    """))
  }
