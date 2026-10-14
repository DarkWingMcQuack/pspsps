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

package pspsps.builders

/** Plain Scala Either staging keeps both branch expressions by-name until completion. */
final class PureEitherCondition private[builders] (condition: Boolean) {
  def withLeft[A](left: => A): PureEitherWithLeft[A] =
    new PureEitherWithLeft(condition, () => left)

  def withRight[B](right: => B): PureEitherWithRight[B] =
    new PureEitherWithRight(condition, () => right)
}

final class PureEitherWithLeft[A] private[builders] (
    condition: Boolean,
    left: () => A
) {
  def withRight[B](right: => B): scala.util.Either[A, B] =
    scala.util.Either.cond(condition, right, left())
}

final class PureEitherWithRight[B] private[builders] (
    condition: Boolean,
    right: () => B
) {
  def withLeft[A](left: => A): scala.util.Either[A, B] =
    scala.util.Either.cond(condition, right(), left)
}

/** Some(null) is retained, matching Option.when rather than Option.apply. */
final class PureOptionCondition private[builders] (condition: Boolean) {
  def withSome[A](some: => A): scala.Option[A] = scala.Option.when(condition)(some)
}

object Either {
  def cond(condition: Boolean): PureEitherCondition = new PureEitherCondition(condition)
}

object Option {
  def cond(condition: Boolean): PureOptionCondition = new PureOptionCondition(condition)
}
