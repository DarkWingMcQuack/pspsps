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

/** Everyday pipeline constructors. Add focused imports for specialised syntax. */
trait CommonSyntax
    extends OptionTSyntax
    with EitherTSyntax
    with NullableSyntax
    with EitherConditionalSyntax
    with OptionConditionalSyntax
    with BasicResourceSyntax
    with RefSyntax
    with StreamSyntax
    with ChunkSyntax

object common extends CommonSyntax
