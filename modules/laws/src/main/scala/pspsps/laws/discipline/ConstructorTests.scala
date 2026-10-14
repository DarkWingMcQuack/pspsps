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

import cats.{Applicative, Eq}
import cats.laws.IsEq
import pspsps.laws.ConstructorLaws
import org.scalacheck.{Arbitrary, Prop}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Reusable Discipline checks for the two overloaded transformer families. */
trait ConstructorTests[F[_]] extends Laws {
  def laws: ConstructorLaws[F]

  private def holds[A: Eq](law: IsEq[A]): Prop =
    Prop(Eq[A].eqv(law.lhs, law.rhs))

  def optionT[A: Arbitrary](implicit
      evidence1: Arbitrary[F[Option[A]]],
      evidence2: Arbitrary[F[A]],
      evidence3: Eq[F[Option[A]]]
  ): RuleSet =
    new DefaultRuleSet(
      name = "optionT",
      parent = None,
      "wrap round trip" -> forAll((value: F[Option[A]]) => holds(laws.optionWrap(value))),
      "fromOption coherence" -> forAll((value: Option[A]) => holds(laws.optionFromOption(value))),
      "liftF coherence" -> forAll((value: F[A]) => holds(laws.optionLiftF(value))),
      "pure coherence" -> forAll((value: A) => holds(laws.optionPure(value))),
      "map coherence" -> forAll((value: F[Option[A]], result: A) =>
        holds(laws.optionMap(value, (_: A) => result))
      )
    )

  def eitherT[E: Arbitrary, A: Arbitrary](implicit
      evidence4: Arbitrary[F[Either[E, A]]],
      evidence5: Arbitrary[F[A]],
      evidence6: Eq[F[Either[E, A]]]
  ): RuleSet =
    new DefaultRuleSet(
      name = "eitherT",
      parent = None,
      "wrap round trip" -> forAll((value: F[Either[E, A]]) => holds(laws.eitherWrap(value))),
      "fromEither coherence" -> forAll((value: Either[E, A]) =>
        holds(laws.eitherFromEither(value))
      ),
      "liftF coherence" -> forAll((value: F[A]) => holds(laws.eitherLiftF[E, A](value))),
      "pure coherence" -> forAll((value: A) => holds(laws.eitherPure[E, A](value))),
      "map coherence" -> forAll((value: F[Either[E, A]], result: A) =>
        holds(laws.eitherMap(value, (_: A) => result))
      )
    )

}

object ConstructorTests {
  def apply[F[_]: Applicative]: ConstructorTests[F] =
    new ConstructorTests[F] {
      val laws: ConstructorLaws[F] = ConstructorLaws[F]
    }
}
