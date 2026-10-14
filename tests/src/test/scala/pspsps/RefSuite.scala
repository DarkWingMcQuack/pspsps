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
import munit.CatsEffectSuite

class RefSuite extends CatsEffectSuite {
  test("Ref allocation creates independent references on each execution") {
    val allocate = 0.asRef[IO]
    for {
      first <- allocate
      second <- allocate
      _ <- first.set(42)
      a <- first.get
      b <- second.get
    } yield {
      assertEquals(a, 42)
      assertEquals(b, 0)
    }
  }

  test("Ref.ofEffect evaluates the initial value in F") {
    for {
      count <- 0.asRef[IO]
      allocate = count.updateAndGet(_ + 1).asRefOfEffect
      before <- count.get
      first <- allocate
      second <- allocate
      a <- first.get
      b <- second.get
    } yield {
      assertEquals(before, 0)
      assertEquals(a, 1)
      assertEquals(b, 2)
    }
  }
}
