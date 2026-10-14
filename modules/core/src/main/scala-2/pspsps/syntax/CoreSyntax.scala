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

/** All syntax provided by pspsps-core; no Cats Effect or FS2 dependency. */
trait CoreSyntax
    extends OptionTSyntax
    with NullableSyntax
    with EitherTSyntax
    with IorTSyntax
    with WriterTSyntax
    with KleisliSyntax
    with StateTSyntax
    with EitherConditionalSyntax
    with OptionConditionalSyntax

object core extends CoreSyntax
