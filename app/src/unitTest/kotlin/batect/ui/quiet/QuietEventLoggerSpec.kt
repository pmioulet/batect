/*
    Copyright 2017-2021 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package batect.ui.quiet

import batect.execution.PostTaskManualCleanup
import batect.execution.model.events.TaskFailedEvent
import batect.testutils.createForEachTest
import batect.testutils.on
import batect.ui.Console
import batect.ui.FailureErrorMessageFormatter
import batect.ui.text.TextRun
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

object QuietEventLoggerSpec : Spek({
    describe("a quiet event logger") {
        val failureErrorMessageFormatter by createForEachTest { mock<FailureErrorMessageFormatter>() }
        val errorConsole by createForEachTest { mock<Console>() }

        val logger by createForEachTest { QuietEventLogger(failureErrorMessageFormatter, errorConsole, mock()) }

        on("when a 'task failed' event is posted") {
            beforeEachTest {
                val event = mock<TaskFailedEvent>()
                whenever(failureErrorMessageFormatter.formatErrorMessage(event)).doReturn(TextRun("Something went wrong."))

                logger.postEvent(event)
            }

            it("prints the message to the console") {
                inOrder(errorConsole) {
                    verify(errorConsole).println()
                    verify(errorConsole).println(TextRun("Something went wrong."))
                }
            }
        }

        on("when a task fails") {
            beforeEachTest { logger.onTaskFailed("some-task", PostTaskManualCleanup.Required.DueToCleanupFailure(emptyList()), emptySet()) }

            it("does not print anything to the console") {
                verifyNoInteractions(errorConsole)
            }
        }

        on("when a task starts") {
            beforeEachTest { logger.onTaskStarting("some-task") }

            it("does not print anything to the console") {
                verifyNoInteractions(errorConsole)
            }
        }

        on("when a task finishes") {
            beforeEachTest { logger.onTaskFinished("some-task", 123, Duration.ofNanos(456)) }

            it("does not print anything to the console") {
                verifyNoInteractions(errorConsole)
            }
        }

        on("when a task finishes with cleanup disabled") {
            beforeEachTest { logger.onTaskFinishedWithCleanupDisabled(PostTaskManualCleanup.Required.DueToTaskSuccessWithCleanupDisabled(emptyList()), emptySet()) }

            it("does not print anything to the console") {
                verifyNoInteractions(errorConsole)
            }
        }
    }
})
