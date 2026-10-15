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

package pspsps.examples

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import pspsps.syntax.common.*
import fs2.Stream

object Main extends IOApp.Simple:
  final case class Content(value: String)

  trait Store:
    def current(key: String, content: Content): IO[Option[Content]]
    def save(content: Content): IO[Content]
    def close: IO[Unit]

  private val acquire: IO[Store] = IO.delay(new Store:
    def current(key: String, content: Content): IO[Option[Content]] =
      key.nonEmpty.asOptionCond.withSome(content).pure[IO]
    def save(content: Content): IO[Content] = content.pure[IO]
    def close: IO[Unit] = ().pure[IO]
  )

  private def program(key: String, content: Content): Stream[IO, Content] =
    acquire
      .asResourceMake(_.close)
      .asStreamResource
      .flatMap { store =>
        store
          .current(key, content)
          .asOptionT
          .filter(_.value.nonEmpty)
          .semiflatMap(store.save)
          .value
          .asStreamEval
          .unNone
      }

  val run: IO[Unit] =
    program("document-1", Content("hello"))
      .evalMap(value => IO.println(value.value))
      .compile
      .drain
