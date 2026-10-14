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

class ImportContractSuite extends FunSuite {
  test("common exposes everyday constructors alongside Cats syntax") {
    assertEquals(
      compileErrors("""
      import cats.effect.IO
      import cats.syntax.all._
      import pspsps.syntax.common._
      val wrapped: cats.data.OptionT[IO, Int] = IO.pure(Option(42)).asOptionT
      val lifted: cats.data.OptionT[IO, Int] = Option(42).asOptionT[IO]
      val either: cats.data.EitherT[IO, String, Int] = IO.pure(42).asEitherTLiftF[String]
      val resource = IO.pure(42).asResourceMake(_ => IO.unit)
      val stream = resource.asStreamResource
      val condition = IO.pure(true).asEitherTCond.withLeft("no").withRight(42)
      val javaValue: String = null
      val optional: Option[String] = javaValue.nullable
    """),
      ""
    )
  }

  test("common leaves specialised constructors out of scope") {
    assert(compileErrors("""
      import cats.effect.IO
      import pspsps.syntax.common._
      42.asIorTPure[IO, String]
    """).nonEmpty)
    assert(compileErrors("""
      import cats.effect.IO
      import pspsps.syntax.common._
      IO.pure((42, IO.unit)).asResourceWrap
    """).nonEmpty)
    assert(compileErrors("""
      import cats.effect.IO
      import cats.effect.kernel.Poll
      import pspsps.syntax.common._
      val acquire: Poll[IO] => IO[Int] = _ => IO.pure(42)
      acquire.asResourceMakeFull(_ => IO.unit)
    """).nonEmpty)
  }

  test("focused imports extend common without changing existing constructors") {
    assertEquals(
      compileErrors("""
      import cats.effect.IO
      import cats.effect.kernel.Poll
      import pspsps.syntax.common._
      import pspsps.syntax.iorT._
      import pspsps.syntax.resourceInterop._
      import pspsps.syntax.resourceAdvanced._
      val ior = 42.asIorTPure[IO, String]
      val acquire: Poll[IO] => IO[Int] = _ => IO.pure(42)
      val advanced = acquire.asResourceMakeFull(_ => IO.unit)
      val wrapped = IO.pure((42, IO.unit)).asResourceWrap
      val ordinary = IO.pure(42).asResourceEval
    """),
      ""
    )
  }

  test("all remains an exhaustive opt-in") {
    assertEquals(
      compileErrors("""
      import cats.effect.IO
      import cats.effect.kernel.Poll
      import pspsps.syntax.all._
      val ior = 42.asIorTPure[IO, String]
      val pull = IO.pure(42).asPullEval
      val state = 42.asStateTPure[IO, String]
      val acquire: Poll[IO] => IO[Int] = _ => IO.pure(42)
      val advanced = acquire.asResourceMakeFull(_ => IO.unit)
      val wrapped = IO.pure((42, IO.unit)).asResourceWrap
    """),
      ""
    )
  }
}
