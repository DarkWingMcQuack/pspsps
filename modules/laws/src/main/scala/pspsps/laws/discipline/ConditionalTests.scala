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

package pspsps.laws.discipline

import cats.{Eq, Monad}
import cats.laws.IsEq
import pspsps.laws.ConditionalLaws
import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Reusable checks with arbitrary outer effects, including empty and failed conditions. */
trait ConditionalTests[F[_]] extends Laws {
  def laws: ConditionalLaws[F]

  private def holds[A: Eq](law: IsEq[A]): Prop =
    Prop(Eq[A].eqv(law.lhs, law.rhs))

  def eitherT[A, B](implicit
      evidence1: Arbitrary[F[Boolean]],
      evidence2: Arbitrary[F[A]],
      evidence3: Arbitrary[F[B]],
      evidence4: Eq[F[Either[A, B]]]
  ): RuleSet =
    new DefaultRuleSet(
      name = "eitherTConditional",
      parent = None,
      "condition coherence" -> forAll((condition: F[Boolean], left: F[A], right: F[B]) =>
        holds(laws.eitherCondition(condition, left, right))
      ),
      "branch order independence" -> forAll((condition: F[Boolean], left: F[A], right: F[B]) =>
        holds(laws.eitherBranchOrder(condition, left, right))
      ),
      "complement swaps channels" -> forAll((condition: F[Boolean], left: F[A], right: F[B]) =>
        holds(laws.eitherComplement(condition, left, right))
      )
    )

  def optionT[A](implicit
      evidence5: Arbitrary[F[Boolean]],
      evidence6: Arbitrary[F[A]],
      evidence7: Eq[F[Option[A]]]
  ): RuleSet =
    new DefaultRuleSet(
      name = "optionTConditional",
      parent = None,
      "condition coherence" -> forAll((condition: F[Boolean], some: F[A]) =>
        holds(laws.optionCondition(condition, some))
      )
    )

}

object ConditionalTests {
  def apply[F[_]: Monad]: ConditionalTests[F] =
    new ConditionalTests[F] {
      val laws: ConditionalLaws[F] = ConditionalLaws[F]
    }
}
