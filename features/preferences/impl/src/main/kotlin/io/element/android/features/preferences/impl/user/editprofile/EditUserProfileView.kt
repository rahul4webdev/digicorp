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

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.Async
import io.element.android.libraries.designsystem.components.LabelledOutlinedTextField
import io.element.android.libraries.designsystem.components.ProgressDialog
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.preview.DayNightPreviews
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.ui.components.AvatarActionBottomSheet
import io.element.android.libraries.matrix.ui.components.EditableAvatarView
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditUserProfileView(
    state: EditUserProfileState,
    onBackPressed: () -> Unit,
    onProfileEdited: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val itemActionsBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    fun onAvatarClicked() {
        focusManager.clearFocus()
        coroutineScope.launch {
            itemActionsBottomSheetState.show()
        }
    }

    Scaffold(
        modifier = modifier.clearFocusOnTap(focusManager),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_edit_profile_title),
                        style = ElementTheme.typography.aliasScreenTitle,
                    )
                },
                navigationIcon = { BackButton(onClick = onBackPressed) },
                actions = {
                    TextButton(
                        text = stringResource(CommonStrings.action_save),
                        enabled = state.saveButtonEnabled,
                        onClick = {
                            focusManager.clearFocus()
                            state.eventSink(EditUserProfileEvents.Save)
                        },
                    )
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            EditableAvatarView(
                userId = state.userId?.value,
                displayName = state.displayName,
                avatarUrl = state.userAvatarUrl,
                avatarSize = AvatarSize.RoomHeader,
                onAvatarClicked = { onAvatarClicked() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(16.dp))
            state.userId?.let {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it.value,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            LabelledOutlinedTextField(
                label = stringResource(R.string.screen_edit_profile_display_name),
                value = state.displayName,
                placeholder = stringResource(CommonStrings.common_room_name_placeholder),
                singleLine = true,
                onValueChange = { state.eventSink(EditUserProfileEvents.UpdateDisplayName(it)) },
            )
        }

        AvatarActionBottomSheet(
            actions = state.avatarActions,
            modalBottomSheetState = itemActionsBottomSheetState,
            onActionSelected = { state.eventSink(EditUserProfileEvents.HandleAvatarAction(it)) }
        )

        when (state.saveAction) {
            is Async.Loading -> {
                ProgressDialog(text = stringResource(R.string.screen_edit_profile_updating_details))
            }
            is Async.Failure -> {
                ErrorDialog(
                    title = stringResource(R.string.screen_edit_profile_error_title),
                    content = stringResource(R.string.screen_edit_profile_error),
                    onDismiss = { state.eventSink(EditUserProfileEvents.CancelSaveChanges) },
                )
            }
            is Async.Success -> {
                LaunchedEffect(state.saveAction) {
                    onProfileEdited()
                }
            }
            else -> Unit
        }
    }
}

private fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier =
    pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
        })
    }

@DayNightPreviews
@Composable
internal fun EditUserProfileViewPreview(@PreviewParameter(EditUserProfileStateProvider::class) state: EditUserProfileState) =
    ElementPreview {
        EditUserProfileView(
            onBackPressed = {},
            onProfileEdited = {},
            state = state,
        )
    }

