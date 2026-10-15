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

import cats.data.{EitherT, Kleisli, OptionT, StateT, WriterT}
import cats.effect.kernel.Resource
import pspsps.syntax.all.*
import fs2.{Pull, RaiseThrowable, Stream}
import munit.FunSuite
import scala.compiletime.testing.typeChecks

/** Constructors require no instances for F except explicit error-raising evidence. */
object CompileContracts:
  def option[F[_], A](value: F[Option[A]]): OptionT[F, A] = value.asOptionT
  def either[F[_], E, A](value: F[Either[E, A]]): EitherT[F, E, A] = value.asEitherT
  def writer[F[_], L, A](value: F[(L, A)]): WriterT[F, L, A] = value.asWriterT
  def kleisli[F[_], R, A](value: R => F[A]): Kleisli[F, R, A] = value.asKleisli
  def state[F[_], S, A](value: F[S => F[(S, A)]]): StateT[F, S, A] = value.asStateTApplyF
  def stream[F[_], A](value: F[A]): Stream[F, A] = value.asStreamEval
  def attempt[F[_], A](value: F[A]): Stream[F, Either[Throwable, A]] = value.asStreamAttemptEval
  def optionStream[F[_], A](value: Option[A]): Stream[F, A] = value.asStreamFromOption[F]
  def optionTStream[F[_], A](value: OptionT[F, A]): Stream[F, A] = value.asStreamFromOptionT
  def eitherStream[F[_]: RaiseThrowable, E <: Throwable, A](value: Either[E, A]): Stream[F, A] =
    value.asStreamFromEither[F]
  def eitherTStream[F[_]: RaiseThrowable, E <: Throwable, A](
      value: EitherT[F, E, A]
  ): Stream[F, A] =
    value.asStreamFromEitherT
  def pull[F[_], A](value: F[A]): Pull[F, Nothing, A] = value.asPullEval
  def resource[F[_], A](value: F[A]): Resource[F, A] = value.asResourceEval
  def pureResource[F[_], A](value: A): Resource[F, A] = value.asResourcePure[F]

class CompileContractSuite extends FunSuite:
  test("invariant receivers require no typeclass for wrapping") {
    assert(typeChecks("""
      import pspsps.syntax.all.*
      final case class Box[A](value: A)
      val value = Box(Option(1))
      val result: cats.data.OptionT[Box, Int] = value.asOptionT
      val stream: fs2.Stream[Box, Option[Int]] = value.asStreamEval
      val resource: cats.effect.kernel.Resource[Box, Option[Int]] = value.asResourceEval
    """))
  }

  test("plain Option and Either accept explicit effect parameters") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.all.*
      val option: cats.data.OptionT[IO, Int] = Option(1).asOptionT[IO]
      val input: Either[String, Int] = Right(1)
      val either: cats.data.EitherT[IO, String, Int] = input.asEitherT[IO]
    """))
  }

  test("Some and Right receivers can be widened with explicit target types") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.all.*
      val option: cats.data.OptionT[IO, Int] = Some(1).asOptionT[IO]
      val either: cats.data.EitherT[IO, String, Int] = (Right(1): Either[String, Int]).asEitherT[IO]
    """))
  }

  test("wrapping and lifting nested shapes have unambiguous explicit spellings") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.all.*
      val input: Option[Option[Int]] = Some(Some(1))
      val wrapped: cats.data.OptionT[Option, Int] = input.asOptionTWrap
      val lifted: cats.data.OptionT[IO, Option[Int]] = input.asOptionT[IO]
    """))
  }

  test("focused imports compose without the umbrella import") {
    assert(typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.optionT.*
      import pspsps.syntax.resource.*
      import pspsps.syntax.stream.*
      val result = IO.pure(Option(1)).asOptionT.value.asResourceEval.asStreamResource
    """))
  }

  test("Cats, Cats Effect, and Fluent syntax imports coexist") {
    assert(typeChecks("""
      import cats.effect.kernel.MonadCancelThrow
      import cats.syntax.all.*
      import cats.effect.syntax.all.*
      import pspsps.syntax.all.*
      def program[F[_]: MonadCancelThrow](value: F[Int], cleanup: F[Unit]) =
        value.guarantee(cleanup).map(_ + 1).asStreamEval
    """))
  }

  test("plain values do not silently select transformer pure semantics") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.all.*
      val result = 1.asOptionT[IO]
    """))
  }

  test("Stream.exec rejects non-Unit effects") {
    assert(!typeChecks("""
      import cats.effect.IO
      import pspsps.syntax.all.*
      val result = IO.pure(1).asStreamExec
    """))
  }

  test("stream conversions reject non-Throwable Left values") {
    assert(!typeChecks("""
      import cats.effect.IO
      import cats.syntax.all.*
      import pspsps.syntax.all.*
      val input = "domain".asLeft[Int]
      val result = input.asStreamFromEither[IO]
    """))
    assert(!typeChecks("""
      import cats.effect.IO
      import cats.syntax.all.*
      import pspsps.syntax.all.*
      val input = "domain".asLeft[Int].pure[IO].asEitherT
      val result = input.asStreamFromEitherT
    """))
  }

  test("plain values do not acquire cleanup implicitly") {
    assert(!typeChecks("""
      import pspsps.syntax.all.*
      val result = 42.asResourceEval
    """))
  }

  test("OptionT lift requires Functor rather than pretending arbitrary F is mappable") {
    assert(!typeChecks("""
      import pspsps.syntax.all.*
      final case class Box[A](value: A)
      val result = Box(1).asOptionTLiftF
    """))
  }
