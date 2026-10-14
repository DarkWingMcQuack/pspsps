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

import cats.{Applicative, Functor}
import cats.effect.kernel.{Poll, Resource, Sync}

/** Resource constructors. Every finalization rule is delegated to Cats Effect. */
trait BasicResourceSyntax {
  implicit final class ResourceEffectOps[F[_], A](private val value: F[A]) {

    /** Evaluate an effect within Resource. Registers NO finalizer. */
    def asResourceEval: Resource[F, A] = Resource.eval(value)

    /** Acquire with Cats Effect's standard masked-acquisition semantics. */
    def asResourceMake(release: A => F[Unit])(implicit F: Functor[F]): Resource[F, A] =
      Resource.make(value)(release)

    /** Register an exit-case-aware finalizer. */
    def asResourceMakeCase(release: (A, Resource.ExitCase) => F[Unit])(implicit
        F: Functor[F]
    ): Resource[F, A] = Resource.makeCase(value)(release)
  }

  implicit final class ResourceAutoCloseableOps[F[_], A <: AutoCloseable](
      private val acquire: F[A]
  ) {

    /** Close through Sync.blocking. Acquisition must already be suspended in F. */
    def asResourceAutoCloseable(implicit F: Sync[F]): Resource[F, A] =
      Resource.fromAutoCloseable(acquire)
  }

  implicit final class ResourceFinalizeOps[F[_]](private val release: F[Unit]) {

    /** Register a finalizer without acquiring a value. */
    def asResourceOnFinalize(implicit F: Applicative[F]): Resource[F, Unit] =
      Resource.onFinalize(release)
  }

  implicit final class ResourceFinalizeCaseOps[F[_]](
      private val release: Resource.ExitCase => F[Unit]
  ) {

    /** Register an exit-case-aware finalizer without acquiring a value. */
    def asResourceOnFinalizeCase(implicit F: Applicative[F]): Resource[F, Unit] =
      Resource.onFinalizeCase(release)
  }

  implicit final class ResourceValueOps[A](private val value: A) {

    /** Lift an already-computed value. No suspension and no finalizer. */
    def asResourcePure[F[_]]: Resource[F, A] = Resource.pure[F, A](value)
  }
}

trait ResourceInteropSyntax {
  implicit final class ResourceWrapOps[F[_], A](private val value: F[(A, F[Unit])]) {

    /** Wrap an acquisition that returns a value and its finalizer. */
    def asResourceWrap(implicit F: Functor[F]): Resource[F, A] = Resource(value)
  }

  implicit final class ResourceWrapCaseOps[F[_], A](
      private val value: F[(A, Resource.ExitCase => F[Unit])]
  ) {

    /** Wrap an acquisition that returns an exit-case-aware finalizer. */
    def asResourceWrapCase: Resource[F, A] = Resource.applyCase(value)
  }
}

trait ResourceAdvancedSyntax {
  implicit final class ResourceAcquireOps[F[_], A](private val acquire: Poll[F] => F[A]) {

    /** Allow acquisition to opt into cancellation with Poll. The acquisition must clean up partial
      * work if canceled before returning A.
      */
    def asResourceMakeFull(release: A => F[Unit])(implicit F: Functor[F]): Resource[F, A] =
      Resource.makeFull(acquire)(release)

    /** Cancelable acquisition with an exit-case-aware finalizer. */
    def asResourceMakeCaseFull(release: (A, Resource.ExitCase) => F[Unit])(implicit
        F: Functor[F]
    ): Resource[F, A] = Resource.makeCaseFull(acquire)(release)
  }
}

trait ResourceSyntax
    extends BasicResourceSyntax
    with ResourceInteropSyntax
    with ResourceAdvancedSyntax

object resource extends ResourceSyntax
object resourceInterop extends ResourceInteropSyntax
object resourceAdvanced extends ResourceAdvancedSyntax
