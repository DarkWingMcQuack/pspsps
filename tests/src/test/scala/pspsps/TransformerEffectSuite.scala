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

import cats.effect.{IO, Ref}
import pspsps.syntax.all._
import munit.CatsEffectSuite

class TransformerEffectSuite extends CatsEffectSuite {
  test("OptionT wrapping does not run the input and does not memoize it") {
    for {
      count <- Ref.of[IO, Int](0)
      effect = count.updateAndGet(_ + 1).map(i => Option(i))
      transformed = effect.asOptionT
      before <- count.get
      first <- transformed.value
      second <- transformed.value
    } yield {
      assertEquals(before, 0)
      assertEquals(first, Some(1))
      assertEquals(second, Some(2))
    }
  }

  test("OptionT lift preserves failures in the outer effect") {
    val boom = new RuntimeException("outer")
    IO.raiseError[Int](boom).asOptionTLiftF.value.attempt.map { result =>
      assertEquals(result, Left(boom))
    }
  }

  test("EitherT lift does not turn outer errors into domain Left values") {
    val boom = new RuntimeException("outer")
    IO.raiseError[Int](boom).asEitherTLiftF[String].value.attempt.map { result =>
      assertEquals(result, Left(boom))
    }
  }

  test("attempt then asEitherT captures the outer Throwable explicitly") {
    val boom = new RuntimeException("outer")
    IO.raiseError[Int](boom).attempt.asEitherT.value.map { result =>
      assertEquals(result, Left(boom))
    }
  }

  test("an inner Left remains distinct from an outer effect failure") {
    val input: IO[Either[String, Int]] = IO.pure(Left("domain"))
    input.asEitherT.value.attempt.map { result =>
      assertEquals(result, Right(Left("domain")))
    }
  }
}
