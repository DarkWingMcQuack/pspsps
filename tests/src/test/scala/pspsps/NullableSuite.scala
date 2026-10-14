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

import pspsps.syntax.common._
import munit.FunSuite

class NullableSuite extends FunSuite {
  test("a missing Java map entry becomes None") {
    val values = new java.util.HashMap[String, String]()
    assertEquals(values.get("missing").nullable, None)
  }

  test("a present Java value is preserved") {
    val values = new java.util.HashMap[String, String]()
    val value = new String("present")
    val _ = values.put("key", value)
    assert(values.get("key").nullable.get eq value)
  }

  test("boxed zero is a present value") {
    val value: java.lang.Integer = java.lang.Integer.valueOf(0)
    assertEquals(value.nullable, Some(value))
  }

  test("nullable rejects primitive values") {
    assert(compileErrors("""
      import pspsps.syntax.common._
      0.nullable
    """).nonEmpty)
  }
}
