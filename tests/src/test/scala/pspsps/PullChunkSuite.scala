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

import cats.effect.IO
import pspsps.syntax.all._
import fs2.{Chunk, Pull}
import munit.CatsEffectSuite

class PullChunkSuite extends CatsEffectSuite {
  test("Pull.eval returns a result, not an emitted element") {
    val pull: Pull[IO, Unit, Unit] = IO.pure(42).asPullEval.map(_ => ())
    pull.stream.compile.toList.map { result =>
      assertEquals(result, Nil)
    }
  }

  test("a Pull result can be emitted explicitly") {
    IO.pure(42).asPullEval.flatMap(_.asPullOutput1[IO]).stream.compile.toList.map { result =>
      assertEquals(result, List(42))
    }
  }

  test("Pull.pure returns a result without emitting") {
    val pull: Pull[IO, Unit, Unit] = 42.asPullPure[IO].map(_ => ())
    pull.stream.compile.toList.map { result =>
      assertEquals(result, Nil)
    }
  }

  test("Pull.attemptEval captures failure as its result") {
    val boom = new RuntimeException("pull")
    IO.raiseError[Int](boom)
      .asPullAttemptEval
      .flatMap(_.asPullOutput1[IO])
      .stream
      .compile
      .toList
      .map { result => assertEquals(result, List(Left(boom))) }
  }

  test("Pull.output emits a whole chunk") {
    Chunk(1, 2).asPullOutput[IO].stream.compile.toList.map { result =>
      assertEquals(result, List(1, 2))
    }
  }

  test("Chunk constructors distinguish a sequence from a singleton sequence") {
    assertEquals(List(1, 2).asChunk.toList, List(1, 2))
    assertEquals(List(1, 2).asChunkSingleton.toList, List(List(1, 2)))
  }
}
