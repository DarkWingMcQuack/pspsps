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

package pspsps.syntax

import cats.FlatMap
import pspsps.builders.{EitherCondition, OptionCondition, PureEitherCondition, PureOptionCondition}

trait EitherTConditionalSyntax {
  implicit final class EitherTConditionEffectOps[F[_]](private val condition: F[Boolean]) {
    def asEitherTCond(implicit F: FlatMap[F]): EitherCondition[F] =
      pspsps.builders.EitherT.condF(condition)

    def asEitherTCondF(implicit F: FlatMap[F]): EitherCondition[F] =
      pspsps.builders.EitherT.condF(condition)
  }

  implicit final class EitherTConditionBooleanOps(private val condition: Boolean) {
    def asEitherTCond[F[_]]: EitherCondition[F] =
      pspsps.builders.EitherT.condPure[F](condition)

    def asEitherTCondPure[F[_]]: EitherCondition[F] =
      pspsps.builders.EitherT.condPure[F](condition)
  }
}

trait OptionTConditionalSyntax {
  implicit final class OptionTConditionEffectOps[F[_]](private val condition: F[Boolean]) {
    def asOptionTCond(implicit F: FlatMap[F]): OptionCondition[F] =
      pspsps.builders.OptionT.condF(condition)

    def asOptionTCondF(implicit F: FlatMap[F]): OptionCondition[F] =
      pspsps.builders.OptionT.condF(condition)
  }

  implicit final class OptionTConditionBooleanOps(private val condition: Boolean) {
    def asOptionTCond[F[_]]: OptionCondition[F] =
      pspsps.builders.OptionT.condPure[F](condition)

    def asOptionTCondPure[F[_]]: OptionCondition[F] =
      pspsps.builders.OptionT.condPure[F](condition)
  }
}

trait EitherConditionalSyntax {
  implicit final class EitherConditionBooleanOps(private val condition: Boolean) {
    def asEitherCond: PureEitherCondition =
      pspsps.builders.Either.cond(condition)
  }
}

trait OptionConditionalSyntax {
  implicit final class OptionConditionBooleanOps(private val condition: Boolean) {
    def asOptionCond: PureOptionCondition =
      pspsps.builders.Option.cond(condition)
  }
}

trait ConditionalSyntax
    extends EitherTConditionalSyntax
    with OptionTConditionalSyntax
    with EitherConditionalSyntax
    with OptionConditionalSyntax

object conditional extends ConditionalSyntax
object either extends EitherConditionalSyntax
object option extends OptionConditionalSyntax
