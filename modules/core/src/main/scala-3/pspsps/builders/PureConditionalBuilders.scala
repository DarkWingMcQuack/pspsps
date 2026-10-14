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

/** A staged conditional for Scala's Either. true selects Right in either order.
  *
  * The Boolean is already evaluated. Payloads remain by-name until the second branch completes the
  * builder. No Cats instance or effect execution is needed.
  */
final class PureEitherCondition private[builders] (condition: Boolean):
  def withLeft[A](left: => A): PureEitherWithLeft[A] =
    new PureEitherWithLeft(condition, () => left)

  def withRight[B](right: => B): PureEitherWithRight[B] =
    new PureEitherWithRight(condition, () => right)

/** Only the missing branch can complete this stage. */
final class PureEitherWithLeft[A] private[builders] (
    condition: Boolean,
    left: () => A
):
  def withRight[B](right: => B): scala.util.Either[A, B] =
    scala.util.Either.cond(condition, right, left())

/** Supplying Right first does not invert the meaning of the condition. */
final class PureEitherWithRight[B] private[builders] (
    condition: Boolean,
    right: () => B
):
  def withLeft[A](left: => A): scala.util.Either[A, B] =
    scala.util.Either.cond(condition, right(), left)

/** false returns None without evaluating the payload; true returns Some, including Some(null). This
  * is Option.when semantics, not Option.apply.
  */
final class PureOptionCondition private[builders] (condition: Boolean):
  def withSome[A](some: => A): scala.Option[A] =
    scala.Option.when(condition)(some)

/** The result is Scala's Either, not a wrapper type. */
type Either[+A, +B] = scala.util.Either[A, B]

/** Opt-in staged conditional factory. This is not the standard-library companion. */
object Either:
  def cond(condition: Boolean): PureEitherCondition =
    new PureEitherCondition(condition)

/** The result is Scala's Option, not a wrapper type. */
type Option[+A] = scala.Option[A]

/** Opt-in staged conditional factory. */
object Option:
  def cond(condition: Boolean): PureOptionCondition =
    new PureOptionCondition(condition)
