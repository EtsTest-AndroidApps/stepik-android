package org.stepik.android.view.step_quiz.ui.delegate

import android.os.Build
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updateMarginsRelative
import com.google.android.material.button.MaterialButton
import org.stepic.droid.R
import org.stepik.android.domain.step_quiz.model.StepQuizLessonData
import org.stepik.android.model.DiscountingPolicyType
import org.stepik.android.model.ReviewStrategyType
import org.stepik.android.model.Step
import org.stepik.android.model.Submission
import org.stepik.android.presentation.step_quiz.StepQuizFeature
import org.stepik.android.presentation.step_quiz.model.ReplyResult
import org.stepik.android.view.step_quiz.mapper.StepQuizFeedbackMapper
import org.stepik.android.view.step_quiz.model.StepQuizFeedbackState
import org.stepik.android.view.step_quiz.resolver.StepQuizFormResolver
import ru.nobird.android.view.base.ui.extension.toPx

class StepQuizDelegate(
    private val step: Step,
    private val stepQuizLessonData: StepQuizLessonData,
    private val stepQuizFormDelegate: StepQuizFormDelegate,
    private val stepQuizFeedbackBlocksDelegate: StepQuizFeedbackBlocksDelegate,

    private val stepQuizActionButton: MaterialButton,
    private val stepRetryButton: MaterialButton,
    private val stepQuizDiscountingPolicy: TextView,
    private val stepQuizReviewTeacherMessage: TextView?,

    private val onNewMessage: (StepQuizFeature.Message) -> Unit,
    /**
     * If null so there is no next action will be shown
     */
    private val onNextClicked: (() -> Unit)? = null
) {
    private val context = stepQuizActionButton.context

    private val stepQuizFeedbackMapper = StepQuizFeedbackMapper()

    private var currentState: StepQuizFeature.State.AttemptLoaded? = null

    init {
        stepQuizActionButton.setOnClickListener { onActionButtonClicked() }
        stepRetryButton.setOnClickListener { onNewMessage(StepQuizFeature.Message.CreateAttemptClicked(step)) }
    }

    fun onActionButtonClicked() {
        val state = currentState ?: return

        if (StepQuizFormResolver.isSubmissionInTerminalState(state)) {
            if (StepQuizFormResolver.canMoveToNextStep(step, stepQuizLessonData, state) && onNextClicked != null) {
                onNextClicked.invoke()
            } else {
                onNewMessage(StepQuizFeature.Message.CreateAttemptClicked(step))
            }
        } else {
            val replyResult = stepQuizFormDelegate.createReply()
            when (replyResult.validation) {
                is ReplyResult.Validation.Success ->
                    onNewMessage(StepQuizFeature.Message.CreateSubmissionClicked(step, replyResult.reply))

                is ReplyResult.Validation.Error ->
                    stepQuizFeedbackBlocksDelegate.setState(StepQuizFeedbackState.Validation(replyResult.validation.message))
            }
        }
    }

    fun setState(state: StepQuizFeature.State.AttemptLoaded) {
        currentState = state

        stepQuizFeedbackBlocksDelegate.setState(stepQuizFeedbackMapper.mapToStepQuizFeedbackState(step.block?.name, state))
        stepQuizFormDelegate.setState(state)

        if (StepQuizFormResolver.canOnlyRetry(step, stepQuizLessonData, state)) {
            stepQuizActionButton.isVisible = false

            stepRetryButton.setText(R.string.step_quiz_action_button_try_again)
            stepRetryButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                updateMargins(right = 0)
                if (Build.VERSION.SDK_INT >= 17) {
                    updateMarginsRelative(end = 0)
                }
            }
        } else {
            stepQuizActionButton.isVisible = true
            stepQuizActionButton.isEnabled = StepQuizFormResolver.isQuizActionEnabled(state)
            stepQuizActionButton.text = resolveQuizActionButtonText(state)

            stepRetryButton.text = null
            stepRetryButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = context.resources.getDimensionPixelOffset(R.dimen.step_submit_button_height)
                updateMargins(right = 16.toPx())
                if (Build.VERSION.SDK_INT >= 17) {
                    updateMarginsRelative(end = 16.toPx())
                }
            }
        }

        stepRetryButton.isVisible =
            StepQuizFormResolver.canMoveToNextStep(step, stepQuizLessonData, state) ||
            StepQuizFormResolver.canOnlyRetry(step, stepQuizLessonData, state)

        val isNeedShowDiscountingPolicy =
            state.restrictions.discountingPolicyType != DiscountingPolicyType.NoDiscount &&
            (state.submissionState as? StepQuizFeature.SubmissionState.Loaded)?.submission?.status != Submission.Status.CORRECT

        stepQuizDiscountingPolicy.isVisible = isNeedShowDiscountingPolicy
        stepQuizDiscountingPolicy.text = resolveQuizDiscountingPolicyText(state)

        stepQuizReviewTeacherMessage?.isVisible = step.actions?.doReview != null && stepQuizLessonData.isTeacher
        step.instructionType?.let { instructionType ->
            @StringRes
            val stringRes =
                when (instructionType) {
                    ReviewStrategyType.PEER ->
                        R.string.step_quiz_review_teacher_peer_message

                    ReviewStrategyType.INSTRUCTOR ->
                        R.string.step_quiz_review_teacher_instructor_message
                }
            stepQuizReviewTeacherMessage?.text = context.getString(stringRes)
        }
    }

    private fun resolveQuizActionButtonText(state: StepQuizFeature.State.AttemptLoaded): String =
        with(state.restrictions) {
            if (StepQuizFormResolver.isSubmissionInTerminalState(state)) {
                when {
                    StepQuizFormResolver.canMoveToNextStep(step, stepQuizLessonData, state) && onNextClicked != null ->
                        context.getString(R.string.next)

                    maxSubmissionCount in 0 until submissionCount ->
                        context.getString(R.string.step_quiz_action_button_no_submissions)

                    else ->
                        context.getString(R.string.step_quiz_action_button_try_again)
                }
            } else {
                if (maxSubmissionCount > submissionCount) {
                    val submissionsLeft = maxSubmissionCount - submissionCount
                    context.getString(
                        R.string.step_quiz_action_button_submit_with_counter,
                        context.resources.getQuantityString(R.plurals.submissions, submissionsLeft, submissionsLeft)
                    )
                } else {
                    context.getString(R.string.step_quiz_action_button_submit)
                }
            }
        }

    private fun resolveQuizDiscountingPolicyText(state: StepQuizFeature.State.AttemptLoaded): String? =
        with(state.restrictions) {
            when (discountingPolicyType) {
                DiscountingPolicyType.Inverse ->
                    context.getString(R.string.discount_policy_inverse_title)

                DiscountingPolicyType.FirstOne, DiscountingPolicyType.FirstThree -> {
                    val remainingSubmissionCount = discountingPolicyType.numberOfTries() - state.restrictions.submissionCount
                    if (remainingSubmissionCount > 0) {
                        context.resources.getQuantityString(R.plurals.discount_policy_first_n, remainingSubmissionCount, remainingSubmissionCount)
                    } else {
                        context.getString(R.string.discount_policy_no_way)
                    }
                }

                else ->
                    null
            }
        }
}