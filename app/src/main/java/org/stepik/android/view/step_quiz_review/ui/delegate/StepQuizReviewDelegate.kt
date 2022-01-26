package org.stepik.android.view.step_quiz_review.ui.delegate

import android.view.View
import android.view.ViewGroup
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.error_no_connection_with_button_small.view.*
import kotlinx.android.synthetic.main.fragment_step_quiz_review_peer.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_footer.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_header.*
import org.stepic.droid.R
import org.stepik.android.model.ReviewStrategyType
import org.stepik.android.model.Submission
import org.stepik.android.presentation.step_quiz.StepQuizFeature
import org.stepik.android.presentation.step_quiz_review.StepQuizReviewFeature
import org.stepik.android.view.progress.ui.mapper.ProgressTextMapper
import org.stepik.android.view.step_quiz.mapper.StepQuizFeedbackMapper
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizDelegate
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFeedbackBlocksDelegate
import org.stepik.android.view.step_quiz_review.ui.widget.ReviewStatusView
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.app.core.model.safeCast

class StepQuizReviewDelegate(
    override val containerView: View,
    private val instructionType: ReviewStrategyType,
    private val actionListener: ActionListener,

    private val blockName: String?,
    private val quizView: View,
    private val quizDelegate: StepQuizDelegate,
    private val quizFeedbackBlocksDelegate: StepQuizFeedbackBlocksDelegate
) : LayoutContainer {
    private val stepQuizFeedbackMapper = StepQuizFeedbackMapper()
    private val resources = containerView.resources

    private val step1viewStateDelegate = ViewStateDelegate<StepQuizReviewFeature.State>()
        .apply {
            addState<StepQuizReviewFeature.State.SubmissionNotMade>(
                reviewStep1DividerBottom, reviewStep1Container, reviewStep1Discounting,
                reviewStep1ActionButton, reviewStep1ActionRetry
            )
        }

    private val step1QuizViewStateDelegate = ViewStateDelegate<StepQuizFeature.State>()
        .apply {
            addState<StepQuizFeature.State.Loading>(stepQuizProgress)
            addState<StepQuizFeature.State.AttemptLoading>(stepQuizProgress)
            addState<StepQuizFeature.State.AttemptLoaded>(reviewStep1Discounting, reviewStep1QuizContainer, reviewStep1ActionButton, reviewStep1ActionRetry)
            addState<StepQuizFeature.State.NetworkError>(stepQuizNetworkError)
        }

    private val step2viewStateDelegate = ViewStateDelegate<StepQuizReviewFeature.State>()
        .apply {
            addState<StepQuizReviewFeature.State.SubmissionNotSelected>(
                reviewStep2DividerBottom, reviewStep2Container, reviewStep2Loading,
                reviewStep2CreateSession, reviewStep2SelectSubmission, reviewStep2Retry
            )
            addState<StepQuizReviewFeature.State.SubmissionSelected>(reviewStep2DividerBottom, reviewStep2Container)
            addState<StepQuizReviewFeature.State.Completed>(reviewStep2DividerBottom, reviewStep2Container)
        }

    init {
        stepQuizNetworkError.tryAgain.setOnClickListener { actionListener.onQuizTryAgainClicked() }

        reviewStep2SelectSubmission.setOnClickListener { actionListener.onSelectDifferentSubmissionClicked() }
        reviewStep2CreateSession.setOnClickListener { actionListener.onCreateSessionClicked() }
        reviewStep2Retry.setOnClickListener { actionListener.onSolveAgainClicked() }

        if (instructionType == ReviewStrategyType.PEER) {
            reviewStep3Container.setOnClickListener { actionListener.onStartReviewClicked() }
        }
    }

    fun render(state: StepQuizReviewFeature.State) {
        if (state is StepQuizReviewFeature.State.WithQuizState) {
            quizFeedbackBlocksDelegate.setState(stepQuizFeedbackMapper.mapToStepQuizFeedbackState(blockName, state.quizState))
        }

        renderStep1(state)
        renderStep2(state)

        if (instructionType == ReviewStrategyType.PEER) {
            renderStep3(state)
            renderStep4(state)
        }

        renderStep5(state)
    }

    private fun renderStep1(state: StepQuizReviewFeature.State) {
        step1viewStateDelegate.switchState(state)
        when (state) {
            is StepQuizReviewFeature.State.SubmissionNotMade -> {
                val submissionStatus = state.quizState.safeCast<StepQuizFeature.State.AttemptLoaded>()
                    ?.submissionState
                    ?.safeCast<StepQuizFeature.SubmissionState.Loaded>()
                    ?.submission
                    ?.status

                reviewStep1Status.status =
                    if (submissionStatus == Submission.Status.WRONG) {
                        ReviewStatusView.Status.ERROR
                    } else {
                        ReviewStatusView.Status.IN_PROGRESS
                    }

                stepQuizDescription.isEnabled = true

                step1QuizViewStateDelegate.switchState(state.quizState)
                if (state.quizState is StepQuizFeature.State.AttemptLoaded) {
                    quizDelegate.setState(state.quizState)
                }

                setQuizViewParent(quizView, reviewStep1QuizContainer)
                setQuizViewParent(quizFeedbackView, reviewStep1QuizContainer)
            }

            else -> {
                stepQuizDescription.isEnabled = false
                reviewStep1Status.status = ReviewStatusView.Status.COMPLETED
            }
        }
    }

    private fun renderStep2(state: StepQuizReviewFeature.State) {
        step2viewStateDelegate.switchState(state)
        when (state) {
            is StepQuizReviewFeature.State.SubmissionNotMade -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_pending)
                setStepStatus(reviewStep2Title, reviewStep2Link, reviewStep2Status, ReviewStatusView.Status.PENDING)
            }
            is StepQuizReviewFeature.State.SubmissionNotSelected -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_in_progress)
                setStepStatus(reviewStep2Title, reviewStep2Link, reviewStep2Status, ReviewStatusView.Status.IN_PROGRESS)

                quizDelegate.setState(state.quizState)

                reviewStep2Loading.isVisible = state.isSessionCreationInProgress
                reviewStep2CreateSession.isVisible = !state.isSessionCreationInProgress
                reviewStep2SelectSubmission.isVisible = !state.isSessionCreationInProgress
                reviewStep2Retry.isVisible = !state.isSessionCreationInProgress

                setQuizViewParent(quizView, reviewStep2Container)
                setQuizViewParent(quizFeedbackView, reviewStep2Container)
            }
            else -> {
                reviewStep2Title.setText(R.string.step_quiz_review_send_completed)
                setStepStatus(reviewStep2Title, reviewStep2Link, reviewStep2Status, ReviewStatusView.Status.COMPLETED)

                state.safeCast<StepQuizReviewFeature.State.WithQuizState>()
                    ?.quizState
                    ?.safeCast<StepQuizFeature.State.AttemptLoaded>()
                    ?.let(quizDelegate::setState)

                setQuizViewParent(quizView, reviewStep2Container)
                setQuizViewParent(quizFeedbackView, reviewStep2Container)
                quizFeedbackView.isVisible = false
            }
        }
    }

    private fun setQuizViewParent(view: View, parent: ViewGroup) {
        val currentParentViewGroup = view.parent.safeCast<ViewGroup>()
        if (currentParentViewGroup == parent) return

        currentParentViewGroup?.removeView(view)
        parent.addView(view)
    }

    private fun renderStep3(state: StepQuizReviewFeature.State) {
        val reviewCount = state.safeCast<StepQuizReviewFeature.State.WithInstruction>()?.instruction?.minReviews ?: 0

        when (state) {
            is StepQuizReviewFeature.State.SubmissionNotMade,
            is StepQuizReviewFeature.State.SubmissionNotSelected -> {
                reviewStep3Title.setText(R.string.step_quiz_review_given_pending_zero)
                setStepStatus(reviewStep3Title, reviewStep3Link, reviewStep3Status, ReviewStatusView.Status.PENDING)
                reviewStep3Container.isVisible = false
                reviewStep3Loading.isVisible = false
            }
            is StepQuizReviewFeature.State.SubmissionSelected -> {
                val givenReviewCount = state.session.givenReviews.size
                val remainingReviewCount = reviewCount - givenReviewCount

                val text =
                    buildString {
                        if (remainingReviewCount > 0) {
                            @PluralsRes
                            val pluralRes =
                                if (givenReviewCount > 0) {
                                    R.plurals.step_quiz_review_given_in_progress
                                } else {
                                    R.plurals.step_quiz_review_given_pending
                                }
                            append(resources.getQuantityString(pluralRes, remainingReviewCount, remainingReviewCount))
                        }

                        if (givenReviewCount > 0) {
                            if (isNotEmpty()) {
                                append(" ")
                            }
                            append(resources.getQuantityString(R.plurals.step_quiz_review_given_completed, givenReviewCount, givenReviewCount))
                        }
                    }

                reviewStep3Title.text = text

                reviewStep3Container.isVisible = remainingReviewCount > 0 && !state.isReviewCreationInProgress

                if (reviewStep3Container.isVisible) {
                    reviewStep3Container.isEnabled = remainingReviewCount <= 0 || state.session.isReviewAvailable
                    reviewStep3Container.setText(if (reviewStep3Container.isEnabled) R.string.step_quiz_review_given_start_review else R.string.step_quiz_review_given_no_review)
                }

                reviewStep3Loading.isVisible = state.isReviewCreationInProgress
                setStepStatus(reviewStep3Title, reviewStep3Link, reviewStep3Status, ReviewStatusView.Status.IN_PROGRESS)
            }
            is StepQuizReviewFeature.State.Completed -> {
                val givenReviewCount = state.session.givenReviews.size

                reviewStep3Title.text = resources.getQuantityString(R.plurals.step_quiz_review_given_completed, givenReviewCount, givenReviewCount)
                reviewStep3Container.isVisible = false
                reviewStep3Loading.isVisible = false
                setStepStatus(reviewStep3Title, reviewStep3Link, reviewStep3Status, ReviewStatusView.Status.COMPLETED)
            }
        }
    }

    private fun renderStep4(state: StepQuizReviewFeature.State) {
        val reviewCount = state.safeCast<StepQuizReviewFeature.State.WithInstruction>()?.instruction?.minReviews ?: 0

        when (state) {
            is StepQuizReviewFeature.State.SubmissionNotMade,
            is StepQuizReviewFeature.State.SubmissionNotSelected -> {
                reviewStep4Title.setText(R.string.step_quiz_review_taken_pending_zero)
                setStepStatus(reviewStep4Title, reviewStep4Link, reviewStep4Status, ReviewStatusView.Status.PENDING)
                reviewStep4Container.isVisible = false
                reviewStep4Hint.isVisible = false
            }
            is StepQuizReviewFeature.State.SubmissionSelected -> {
                val takenReviewCount = state.session.takenReviews.size
                val remainingReviewCount = reviewCount - takenReviewCount

                val text =
                    buildString {
                        if (remainingReviewCount > 0) {
                            @PluralsRes
                            val pluralRes =
                                if (takenReviewCount > 0) {
                                    R.plurals.step_quiz_review_taken_in_progress
                                } else {
                                    R.plurals.step_quiz_review_taken_pending
                                }
                            append(resources.getQuantityString(pluralRes, remainingReviewCount, remainingReviewCount))
                        }

                        if (takenReviewCount > 0) {
                            if (isNotEmpty()) {
                                append(" ")
                            }
                            append(resources.getQuantityString(R.plurals.step_quiz_review_taken_completed, takenReviewCount, takenReviewCount))
                        }
                    }

                val status =
                    if (remainingReviewCount > 0) {
                        ReviewStatusView.Status.IN_PROGRESS
                    } else {
                        ReviewStatusView.Status.COMPLETED
                    }

                reviewStep4Title.text = text
                setStepStatus(reviewStep4Title, reviewStep4Link, reviewStep4Status, status)

                reviewStep4Container.isVisible = takenReviewCount > 0
                reviewStep4Container.setOnClickListener { actionListener.onTakenReviewClicked(state.session.id) }
                reviewStep4Hint.isVisible = takenReviewCount == 0
            }
            is StepQuizReviewFeature.State.Completed -> {
                val takenReviewCount = state.session.takenReviews.size
                reviewStep4Title.text = resources.getQuantityString(R.plurals.step_quiz_review_taken_completed, takenReviewCount, takenReviewCount)
                setStepStatus(reviewStep4Title, reviewStep4Link, reviewStep4Status, ReviewStatusView.Status.COMPLETED)
                reviewStep4Container.isVisible = takenReviewCount > 0
                reviewStep4Container.setOnClickListener { actionListener.onTakenReviewClicked(state.session.id) }
                reviewStep4Hint.isVisible = false
            }
        }
    }

    private fun renderStep5(state: StepQuizReviewFeature.State) {
        reviewStep5Status.position =
            when (instructionType) {
                ReviewStrategyType.PEER -> 5
                ReviewStrategyType.INSTRUCTOR -> 3
            }

        when (state) {
            is StepQuizReviewFeature.State.Completed -> {
                val receivedPoints = state.progress?.score?.toFloatOrNull() ?: 0f

                reviewStep5Title.text = ProgressTextMapper
                    .mapProgressToText(
                        containerView.context,
                        receivedPoints,
                        state.progress?.cost ?: 0,
                        R.string.step_quiz_review_peer_completed,
                        R.string.step_quiz_review_peer_completed,
                        R.plurals.points
                    )

                when (instructionType) {
                    ReviewStrategyType.PEER ->
                        reviewStep5Container.isVisible = false

                    ReviewStrategyType.INSTRUCTOR -> {
                        reviewStep5Container.setOnClickListener { actionListener.onTakenReviewClicked(state.session.id) }
                        reviewStep5Container.isVisible = true
                    }
                }
                setStepStatus(reviewStep5Title, reviewStep5Link, reviewStep5Status, ReviewStatusView.Status.IN_PROGRESS)
                reviewStep5Status.status = ReviewStatusView.Status.COMPLETED
                reviewStep5Hint.isVisible = false
            }
            else -> {
                val cost = state.safeCast<StepQuizReviewFeature.State.WithProgress>()?.progress?.cost ?: 0L

                @StringRes
                val stringRes =
                    when (instructionType) {
                        ReviewStrategyType.PEER ->
                            R.string.step_quiz_review_peer_pending

                        ReviewStrategyType.INSTRUCTOR ->
                            R.string.step_quiz_review_instructor_pending
                    }

                reviewStep5Title.text = resources.getString(stringRes, resources.getQuantityString(R.plurals.points, cost.toInt(), cost))
                reviewStep5Container.isVisible = false
                val status =
                    if (state is StepQuizReviewFeature.State.SubmissionSelected && instructionType == ReviewStrategyType.INSTRUCTOR) {
                        ReviewStatusView.Status.IN_PROGRESS
                    } else {
                        ReviewStatusView.Status.PENDING
                    }

                reviewStep5Hint.isVisible = instructionType == ReviewStrategyType.INSTRUCTOR && status == ReviewStatusView.Status.IN_PROGRESS

                setStepStatus(reviewStep5Title, reviewStep5Link, reviewStep5Status, status)
            }
        }
    }

    private fun setStepStatus(titleView: View, linkView: View, statusView: ReviewStatusView, status: ReviewStatusView.Status) {
        titleView.isEnabled = status == ReviewStatusView.Status.IN_PROGRESS
        linkView.isEnabled = status.ordinal >= ReviewStatusView.Status.IN_PROGRESS.ordinal
        statusView.status = status
    }

    interface ActionListener {
        fun onSelectDifferentSubmissionClicked()
        fun onCreateSessionClicked()
        fun onSolveAgainClicked()

        fun onQuizTryAgainClicked()

        fun onStartReviewClicked()
        fun onTakenReviewClicked(sessionId: Long)
    }
}