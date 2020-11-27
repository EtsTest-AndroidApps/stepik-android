package org.stepik.android.view.step_quiz.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.android.synthetic.main.error_no_connection_with_button_small.view.*
import kotlinx.android.synthetic.main.fragment_step_quiz.*
import kotlinx.android.synthetic.main.view_step_quiz_submit_button.*
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepic.droid.ui.util.snackbar
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.domain.step_quiz.model.StepQuizLessonData
import org.stepik.android.model.Step
import org.stepik.android.presentation.step_quiz.StepQuizViewModel
import org.stepik.android.presentation.step_quiz.StepQuizFeature
import org.stepik.android.view.in_app_web_view.ui.dialog.InAppWebViewDialogFragment
import org.stepik.android.view.lesson.ui.interfaces.NextMoveable
import org.stepik.android.view.step.routing.StepDeepLinkBuilder
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizDelegate
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFeedbackBlocksDelegate
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFormDelegate
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import ru.nobird.android.presentation.redux.container.ReduxView
import ru.nobird.android.view.base.ui.extension.argument
import ru.nobird.android.view.base.ui.extension.showIfNotExists
import javax.inject.Inject

abstract class DefaultStepQuizFragment : Fragment(), ReduxView<StepQuizFeature.State, StepQuizFeature.Action.ViewAction> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var screenManager: ScreenManager

    @Inject
    internal lateinit var stepDeepLinkBuilder: StepDeepLinkBuilder

    protected lateinit var stepWrapper: StepPersistentWrapper

    @Inject
    internal lateinit var stepWrapperRxRelay: BehaviorRelay<StepPersistentWrapper>
    @Inject
    internal lateinit var lessonData: LessonData

    protected var stepId: Long by argument()

    private val viewModel: StepQuizViewModel by viewModels { viewModelFactory }

    private lateinit var viewStateDelegate: ViewStateDelegate<StepQuizFeature.State>
    private lateinit var stepQuizDelegate: StepQuizDelegate

    protected abstract val quizLayoutRes: Int
        @LayoutRes get

    protected abstract val quizViews: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent()

        stepWrapper = stepWrapperRxRelay.value ?: throw IllegalStateException("Step wrapper cannot be null")

        viewModel.onNewMessage(StepQuizFeature.Message.InitWithStep(stepWrapper, lessonData))
    }

    private fun injectComponent() {
        App.componentManager()
            .stepComponent(stepId)
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        (inflater.inflate(R.layout.fragment_step_quiz, container, false) as ViewGroup)
            .apply {
                addView(inflater.inflate(quizLayoutRes, this, false))
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<StepQuizFeature.State.Idle>()
        viewStateDelegate.addState<StepQuizFeature.State.Loading>(stepQuizProgress)
        viewStateDelegate.addState<StepQuizFeature.State.AttemptLoading>(stepQuizProgress)
        viewStateDelegate.addState<StepQuizFeature.State.AttemptLoaded>(stepQuizDiscountingPolicy, stepQuizFeedbackBlocks, stepQuizDescription, stepQuizActionContainer, *quizViews)
        viewStateDelegate.addState<StepQuizFeature.State.NetworkError>(stepQuizNetworkError)

        stepQuizNetworkError.tryAgain.setOnClickListener {
            viewModel.onNewMessage(StepQuizFeature.Message.InitWithStep(stepWrapper, lessonData, forceUpdate = true))
        }

        stepQuizDelegate =
            StepQuizDelegate(
                step = stepWrapper.step,
                stepQuizLessonData = StepQuizLessonData(lessonData),
                stepQuizFormDelegate = createStepQuizFormDelegate(view),
                stepQuizFeedbackBlocksDelegate =
                    StepQuizFeedbackBlocksDelegate(
                        stepQuizFeedbackBlocks,
                        stepWrapper.step.actions?.doReview != null
                    ) { openStepInWeb(stepWrapper.step) },
                stepQuizActionButton = stepQuizAction,
                stepRetryButton = stepQuizRetry,
                stepQuizDiscountingPolicy = stepQuizDiscountingPolicy,
                onNewMessage = viewModel::onNewMessage
            ) {
                (parentFragment as? NextMoveable)?.moveNext()
            }
    }

    protected abstract fun createStepQuizFormDelegate(view: View): StepQuizFormDelegate

    protected fun onActionButtonClicked() {
        stepQuizDelegate.onActionButtonClicked()
    }

    override fun onStart() {
        super.onStart()
        viewModel.attachView(this)
    }

    override fun onStop() {
        viewModel.detachView(this)
        stepQuizDelegate.syncReplyState()
        super.onStop()
    }

    override fun render(state: StepQuizFeature.State) {
        viewStateDelegate.switchState(state)
        if (state is StepQuizFeature.State.AttemptLoaded) {
            stepQuizDelegate.setState(state)
        }
    }

    override fun onAction(action: StepQuizFeature.Action.ViewAction) {
        if (action is StepQuizFeature.Action.ViewAction.ShowNetworkError) {
            view?.snackbar(messageRes = R.string.no_connection)
        }
    }

    private fun openStepInWeb(step: Step) {
        InAppWebViewDialogFragment
            .newInstance(lessonData.lesson.title.orEmpty(), stepDeepLinkBuilder.createStepLink(step), isProvideAuth = true)
            .showIfNotExists(childFragmentManager, InAppWebViewDialogFragment.TAG)
    }
}