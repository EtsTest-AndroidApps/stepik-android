package org.stepik.android.view.step_quiz_review.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.android.synthetic.main.error_no_connection_with_button_small.view.*
import kotlinx.android.synthetic.main.fragment_step_quiz_review.*
import kotlinx.android.synthetic.main.fragment_step_quiz_review_peer.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_header.*
import kotlinx.android.synthetic.main.layout_step_quiz_review_header.view.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepic.droid.ui.util.snackbar
import org.stepic.droid.util.AppConstants
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.domain.step_quiz.model.StepQuizLessonData
import org.stepik.android.model.ReviewStrategyType
import org.stepik.android.model.Submission
import org.stepik.android.model.attempts.Attempt
import org.stepik.android.presentation.step_quiz.StepQuizFeature
import org.stepik.android.presentation.step_quiz_review.StepQuizReviewViewModel
import org.stepik.android.presentation.step_quiz_review.StepQuizReviewFeature
import org.stepik.android.view.in_app_web_view.ui.dialog.InAppWebViewDialogFragment
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizDelegate
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFeedbackBlocksDelegate
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFormDelegate
import org.stepik.android.view.step_quiz_choice.ui.delegate.ChoiceStepQuizFormDelegate
import org.stepik.android.view.step_quiz_fill_blanks.ui.delegate.FillBlanksStepQuizFormDelegate
import org.stepik.android.view.step_quiz_matching.ui.delegate.MatchingStepQuizFormDelegate
import org.stepik.android.view.step_quiz_review.routing.StepQuizReviewDeepLinkBuilder
import org.stepik.android.view.step_quiz_review.ui.delegate.StepQuizReviewDelegate
import org.stepik.android.view.step_quiz_sorting.ui.delegate.SortingStepQuizFormDelegate
import org.stepik.android.view.step_quiz_table.ui.delegate.TableStepQuizFormDelegate
import org.stepik.android.view.step_quiz_text.ui.delegate.TextStepQuizFormDelegate
import org.stepik.android.view.submission.ui.dialog.SubmissionsDialogFragment
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.presentation.redux.container.ReduxView
import ru.nobird.android.view.base.ui.extension.argument
import ru.nobird.android.view.base.ui.extension.showIfNotExists
import ru.nobird.android.view.redux.ui.extension.reduxViewModel
import javax.inject.Inject

class StepQuizReviewFragment :
    Fragment(),
    ReduxView<StepQuizReviewFeature.State, StepQuizReviewFeature.Action.ViewAction>,
    SubmissionsDialogFragment.Callback {
    companion object {
        val supportedQuizTypes =
            setOf(
                AppConstants.TYPE_STRING,
                AppConstants.TYPE_NUMBER,
                AppConstants.TYPE_MATH,
                AppConstants.TYPE_FREE_ANSWER,
                AppConstants.TYPE_CHOICE,
                AppConstants.TYPE_MATCHING,
                AppConstants.TYPE_SORTING,
                AppConstants.TYPE_FILL_BLANKS,
                AppConstants.TYPE_TABLE
            )

        fun newInstance(stepId: Long, instructionType: ReviewStrategyType): Fragment =
            StepQuizReviewFragment()
                .apply {
                    this.stepId = stepId
                    this.instructionType = instructionType
                }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var stepQuizReviewDeepLinkBuilder: StepQuizReviewDeepLinkBuilder

    @Inject
    internal lateinit var stepWrapperRxRelay: BehaviorRelay<StepPersistentWrapper>

    @Inject
    internal lateinit var lessonData: LessonData

    private var stepId: Long by argument()
    private var instructionType: ReviewStrategyType by argument()

    private lateinit var stepWrapper: StepPersistentWrapper

    private val stepQuizReviewViewModel: StepQuizReviewViewModel by reduxViewModel(this) { viewModelFactory }
    private lateinit var delegate: StepQuizReviewDelegate

    private lateinit var viewStateDelegate: ViewStateDelegate<StepQuizReviewFeature.State>

    private lateinit var quizView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injectComponent()
        stepWrapper = stepWrapperRxRelay.value ?: throw IllegalStateException("Step wrapper cannot be null")
    }

    private fun injectComponent() {
        App.componentManager()
            .stepComponent(stepId)
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_step_quiz_review, container, false) as ViewGroup

        @LayoutRes
        val layoutId =
            when (instructionType) {
                ReviewStrategyType.PEER ->
                    R.layout.fragment_step_quiz_review_peer

                ReviewStrategyType.INSTRUCTOR ->
                    R.layout.fragment_step_quiz_review_instructor
            }

        // we don't pass [root] in order to clear margins
        quizView = inflater.inflate(getLayoutResForStep(stepWrapper.step.block?.name), null)

        inflater.inflate(layoutId, view)
            .also {
                it.reviewStep1Container.addView(quizView)
            }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<StepQuizReviewFeature.State.Idle>(stepQuizReviewLoading)
        viewStateDelegate.addState<StepQuizReviewFeature.State.Loading>(stepQuizReviewLoading)
        viewStateDelegate.addState<StepQuizReviewFeature.State.Error>(stepQuizReviewNetworkError)
        viewStateDelegate.addState<StepQuizReviewFeature.State.SubmissionNotMade>(stepQuizReviewContainer)
        viewStateDelegate.addState<StepQuizReviewFeature.State.SubmissionNotSelected>(stepQuizReviewContainer)
        viewStateDelegate.addState<StepQuizReviewFeature.State.SubmissionSelected>(stepQuizReviewContainer)
        viewStateDelegate.addState<StepQuizReviewFeature.State.Completed>(stepQuizReviewContainer)

        stepQuizReviewNetworkError.tryAgain
            .setOnClickListener { stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.InitWithStep(stepWrapper, lessonData, forceUpdate = true)) }

        val actionListener = object : StepQuizReviewDelegate.ActionListener {
            override fun onSelectDifferentSubmissionClicked() {
                showSubmissions()
            }

            override fun onCreateSessionClicked() {
                stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.CreateSessionWithCurrentSubmission)
            }

            override fun onSolveAgainClicked() {
                stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.SolveAgain(stepWrapper.step))
            }

            override fun onQuizTryAgainClicked() {
                stepQuizReviewViewModel.onNewMessage(
                    StepQuizReviewFeature.Message.StepQuizMessage(StepQuizFeature.Message.InitWithStep(stepWrapper, lessonData, forceUpdate = true))
                )
            }

            override fun onStartReviewClicked() {
                stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.StartReviewWithCurrentSession)
            }

            override fun onTakenReviewClicked(sessionId: Long) {
                openInWeb(
                    R.string.step_quiz_review_taken_title,
                    stepQuizReviewDeepLinkBuilder.createTakenReviewDeepLink(sessionId)
                )
            }
        }

        val blockName = stepWrapper.step.block?.name
        val stepQuizBlockDelegate = StepQuizFeedbackBlocksDelegate(quizFeedbackView, false) {}
        val quizDelegate =
            StepQuizDelegate(
                step = stepWrapper.step,
                stepQuizLessonData = StepQuizLessonData(lessonData),
                stepQuizFormDelegate = getDelegateForStep(blockName, view) ?: throw IllegalStateException("Unsupported quiz"),
                stepQuizFeedbackBlocksDelegate = stepQuizBlockDelegate,

                stepQuizActionButton = reviewStep1ActionButton,
                stepRetryButton = reviewStep1ActionRetry,

                stepQuizDiscountingPolicy = reviewStep1Discounting,
                onNewMessage = { stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.StepQuizMessage(it)) }
            )

        delegate =
            StepQuizReviewDelegate(
                view, instructionType, actionListener,
                blockName,
                quizView,
                quizDelegate,
                stepQuizBlockDelegate
            )
    }

    // todo reduce duplication from SolutionCommentDialogFragment
    @LayoutRes
    private fun getLayoutResForStep(blockName: String?): Int =
        when (blockName) {
            AppConstants.TYPE_STRING,
            AppConstants.TYPE_NUMBER,
            AppConstants.TYPE_MATH,
            AppConstants.TYPE_FREE_ANSWER ->
                R.layout.layout_step_quiz_text

            AppConstants.TYPE_CHOICE ->
                R.layout.layout_step_quiz_choice

            AppConstants.TYPE_SORTING,
            AppConstants.TYPE_MATCHING ->
                R.layout.layout_step_quiz_sorting

            AppConstants.TYPE_FILL_BLANKS ->
                R.layout.layout_step_quiz_fill_blanks

            AppConstants.TYPE_TABLE ->
                R.layout.layout_step_quiz_table

            else ->
                R.layout.fragment_step_quiz_unsupported
        }

    private fun getDelegateForStep(blockName: String?, view: View): StepQuizFormDelegate? =
        when (blockName) {
            AppConstants.TYPE_STRING,
            AppConstants.TYPE_NUMBER,
            AppConstants.TYPE_MATH,
            AppConstants.TYPE_FREE_ANSWER ->
                TextStepQuizFormDelegate(view, blockName)

            AppConstants.TYPE_CHOICE ->
                ChoiceStepQuizFormDelegate(view)

            AppConstants.TYPE_SORTING ->
                SortingStepQuizFormDelegate(view)

            AppConstants.TYPE_MATCHING ->
                MatchingStepQuizFormDelegate(view)

            AppConstants.TYPE_FILL_BLANKS ->
                FillBlanksStepQuizFormDelegate(view, childFragmentManager)

            AppConstants.TYPE_TABLE ->
                TableStepQuizFormDelegate(view, childFragmentManager)

            else ->
                null
        }

    override fun render(state: StepQuizReviewFeature.State) {
        viewStateDelegate.switchState(state)
        delegate.render(state)
    }

    override fun onAction(action: StepQuizReviewFeature.Action.ViewAction) {
        when (action) {
            is StepQuizReviewFeature.Action.ViewAction.ShowNetworkError ->
                view?.snackbar(messageRes = R.string.connectionProblems)

            is StepQuizReviewFeature.Action.ViewAction.OpenReviewScreen ->
                openInWeb(R.string.step_quiz_review_given_title, stepQuizReviewDeepLinkBuilder.createReviewDeepLink(action.reviewId))
        }
    }

    private fun openInWeb(@StringRes titleRes: Int, url: String) {
        InAppWebViewDialogFragment
            .newInstance(getString(titleRes), url, isProvideAuth = true)
            .showIfNotExists(childFragmentManager, InAppWebViewDialogFragment.TAG)
    }

    /**
     * Submission selection
     */
    private fun showSubmissions() {
        SubmissionsDialogFragment
            .newInstance(stepWrapper.step, status = Submission.Status.CORRECT, isSelectionEnabled = true)
            .showIfNotExists(childFragmentManager, SubmissionsDialogFragment.TAG)
    }

    override fun onSubmissionSelected(submission: Submission, attempt: Attempt) {
        stepQuizReviewViewModel.onNewMessage(StepQuizReviewFeature.Message.ChangeSubmission(submission, attempt))
    }
}