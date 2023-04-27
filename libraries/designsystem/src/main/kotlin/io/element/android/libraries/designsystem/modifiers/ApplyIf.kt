/*
 * Copyright (c) 2023 New Vector Ltd
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

package io.element.android.libraries.designsystem.modifiers

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Applies the [ifTrue] modifier when the [condition] is true, [ifFalse] otherwise.
 */
@SuppressLint("UnnecessaryComposedModifier") // It's actually necessary due to the `@Composable` lambdas
fun Modifier.applyIf(
    condition: Boolean,
    ifTrue: @Composable Modifier.() -> Modifier,
    ifFalse: @Composable (Modifier.() -> Modifier)? = null
): Modifier =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "applyIf"
            value = condition
        }
    ) {
        when {
            condition -> then(ifTrue(Modifier))
            ifFalse != null -> then(ifFalse(Modifier))
            else -> this
        }
    }
