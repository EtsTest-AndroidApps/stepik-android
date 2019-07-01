package org.stepik.android.view.step_quiz.ui.delegate

import org.stepik.android.model.Reply
import org.stepik.android.model.Submission
import org.stepik.android.model.attempts.Attempt
import org.stepik.android.presentation.step_quiz_text.TextStepQuizView

interface StepQuizFormDelegate {
    fun setState(state: TextStepQuizView.State)

    /**
     * Generates reply from current form data
     */
    fun createReply(): Reply

    /**
     * Validates form for ability to create a reply
     * @returns null if validation successful or message string otherwise
     */
    fun validateForm(): String?
}