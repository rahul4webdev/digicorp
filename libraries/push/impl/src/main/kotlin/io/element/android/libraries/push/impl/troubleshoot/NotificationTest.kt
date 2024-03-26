/*
 * Copyright (c) 2024 New Vector Ltd
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

package io.element.android.libraries.push.impl.troubleshoot

import com.squareup.anvil.annotations.ContributesMultibinding
import io.element.android.libraries.core.notifications.NotificationTroubleshootTest
import io.element.android.libraries.core.notifications.NotificationTroubleshootTestDelegate
import io.element.android.libraries.core.notifications.NotificationTroubleshootTestState
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.push.impl.notifications.NotificationDisplayer
import io.element.android.libraries.push.impl.notifications.factories.NotificationCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@ContributesMultibinding(AppScope::class)
class NotificationTest @Inject constructor(
    private val notificationCreator: NotificationCreator,
    private val notificationDisplayer: NotificationDisplayer,
    private val notificationClickHandler: NotificationClickHandler
) : NotificationTroubleshootTest {
    override val order = 50
    private val delegate = NotificationTroubleshootTestDelegate(
        defaultName = "Display notification",
        defaultDescription = "Check that the application can display notification",
        fakeDelay = NotificationTroubleshootTestDelegate.SHORT_DELAY,
    )
    override val state: StateFlow<NotificationTroubleshootTestState> = delegate.state

    override suspend fun run(coroutineScope: CoroutineScope) {
        delegate.start()
        val notification = notificationCreator.createDiagnosticNotification()
        val result = notificationDisplayer.displayDiagnosticNotification(notification)
        if (result) {
            coroutineScope.listenToNotificationClick()
            delegate.updateState(
                description = "Please click on the notification to continue the test.",
                status = NotificationTroubleshootTestState.Status.WaitingForUser
            )
        } else {
            delegate.updateState(
                description = "Cannot display the notification.",
                status = NotificationTroubleshootTestState.Status.Failure(false)
            )
        }
    }

    private fun CoroutineScope.listenToNotificationClick() = launch {
        val job = launch {
            notificationClickHandler.state.first()
            Timber.d("Notification clicked!")
        }
        val s = withTimeoutOrNull(30.seconds) {
            job.join()
        }
        job.cancel()
        if (s == null) {
            notificationDisplayer.dismissDiagnosticNotification()
            delegate.updateState(
                description = "The notification has not been clicked.",
                status = NotificationTroubleshootTestState.Status.Failure(false)
            )
        } else {
            delegate.updateState(
                description = "The notification has been clicked!",
                status = NotificationTroubleshootTestState.Status.Success
            )
        }
    }.invokeOnCompletion {
        // Ensure that the notification is cancelled when the screen is left
        notificationDisplayer.dismissDiagnosticNotification()
    }

    override fun reset() = delegate.reset()
}
