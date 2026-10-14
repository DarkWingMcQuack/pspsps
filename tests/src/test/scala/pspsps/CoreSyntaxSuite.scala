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

import cats.data.{EitherT, Ior, OptionT}
import cats.implicits._
import pspsps.syntax.all._
import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class CoreSyntaxSuite extends ScalaCheckSuite {
  test("OptionT inferred wrapping and explicitly selected lifting") {
    val input: List[Option[Int]] = List(Some(1), None)
    val wrapped: OptionT[List, Int] = input.asOptionT
    val lifted: OptionT[List, Int] = Option(2).asOptionT[List]
    assertEquals(wrapped.value, input)
    assertEquals(lifted.value, List(Some(2)))
  }

  test("OptionT explicit aliases select different meanings of nested Option") {
    val input: Option[Option[Int]] = Some(Some(1))
    val wrapped: OptionT[Option, Int] = input.asOptionTWrap
    val lifted: OptionT[List, Option[Int]] = input.toOptionT[List]
    assertEquals(wrapped.value, input)
    assertEquals(lifted.value, List(input))
  }

  test("EitherT inferred wrapping and explicitly selected lifting") {
    val input: List[Either[String, Int]] = List(Right(1), Left("no"))
    val wrapped: EitherT[List, String, Int] = input.asEitherT
    val plain: Either[String, Int] = Right(2)
    val lifted: EitherT[List, String, Int] = plain.asEitherT[List]
    assertEquals(wrapped.value, input)
    assertEquals(lifted.value, List(Right(2)))
  }

  test("EitherT explicit aliases preserve distinct outer and inner errors") {
    type Outer[A] = Either[Boolean, A]
    val input: Outer[Either[String, Int]] = Right(Left("inner"))
    val wrapped: EitherT[Outer, String, Int] = input.asEitherTWrap
    val lifted: EitherT[List, Boolean, Either[String, Int]] = input.toEitherT[List]
    assertEquals(wrapped.value, input)
    assertEquals(lifted.value, List(input))
  }

  test("pure and left constructors select the intended channel") {
    assertEquals(1.asOptionTPure[List].value, List(Some(1)))
    assertEquals(1.asEitherTPure[List, String].value, List(Right(1)))
    assertEquals("bad".asEitherTLeftT[List, Int].value, List(Left("bad")))
    assertEquals(List("bad").asEitherTLeftF[Int].value, List(Left("bad")))
  }

  test("Ior constructors preserve Both as well as Left and Right") {
    val values: List[Ior[String, Int]] = List(Ior.Left("bad"), Ior.Right(1), Ior.Both("warn", 2))
    assertEquals(values.asIorT.value, values)
    values.foreach { value =>
      assertEquals(value.asIorTFromIor[List].value, List(value))
    }
    assertEquals(List(1).asIorTLiftF[String].value, List(Ior.Right(1)))
    assertEquals(1.asIorTPure[List, String].value, List(Ior.Right(1)))
  }

  test("Writer constructors preserve the log and use an empty log only on lifts") {
    val pair = (List("read"), 42)
    assertEquals(List(pair).asWriterT.run, List(pair))
    assertEquals(pair.asWriterTFromTuple[Option].run, Some(pair))
    assertEquals(pair.asWriter.run, pair)
    assertEquals(List(42).asWriterTLiftF[List[String]].run, List((List.empty[String], 42)))
    assertEquals(42.asWriterTValue[Option, List[String]].run, Some((List.empty[String], 42)))
  }

  test("Kleisli construction does not call the function") {
    var calls = 0
    val run: String => Option[Int] = key => { calls += 1; Some(key.length) }
    val k = run.asKleisli
    assertEquals(calls, 0)
    assertEquals(k.run("abc"), Some(3))
    assertEquals(calls, 1)
  }

  test("Reader and Cokleisli retain their functions") {
    val reader: String => Int = _.length
    val cokleisli: List[Int] => Int = _.sum
    assertEquals(reader.asReader.run("abc"), 3)
    assertEquals(cokleisli.asCokleisli.run(List(1, 2)), 3)
  }

  test("Kleisli lifts ignore the environment") {
    assertEquals(List(42).asKleisliLiftF[String].run("ignored"), List(42))
    assertEquals(42.asKleisliPure[Option, String].run("ignored"), Some(42))
  }

  test("State and StateT preserve transitions and untouched state on lifts") {
    val transition: Int => Option[(Int, String)] = s => Some((s + 1, s.toString))
    val pureTransition: Int => (Int, String) = s => (s + 1, s.toString)
    val effectfulTransition: Option[Int => Option[(Int, String)]] = Some(transition)
    assertEquals(transition.asStateT.run(10), Some((11, "10")))
    assertEquals(effectfulTransition.asStateTApplyF.run(10), Some((11, "10")))
    assertEquals(pureTransition.asState.run(10).value, (11, "10"))
    assertEquals(Option(42).asStateTLiftF[String].run("s"), Some(("s", 42)))
    assertEquals(42.asStateTPure[Option, String].run("s"), Some(("s", 42)))
  }

  test("IndexedStateT permits a changed state type") {
    val transition: Int => Option[(String, Boolean)] = s => Some((s.toString, true))
    assertEquals(transition.asIndexedStateT.run(10), Some(("10", true)))
  }

  property("WriterT is a structural round trip") {
    forAll { (input: List[(String, Int)]) => assertEquals(input.asWriterT.run, input) }
  }

  property("OptionT lift agrees with mapping Some") {
    forAll { (input: List[Int]) =>
      assertEquals(input.asOptionTLiftF.value, input.map(i => Some(i): Option[Int]))
    }
  }

  property("EitherT lift agrees with mapping Right") {
    forAll { (input: List[Int]) =>
      assertEquals(
        input.asEitherTLiftF[String].value,
        input.map(i => Right(i): Either[String, Int])
      )
    }
  }
}
