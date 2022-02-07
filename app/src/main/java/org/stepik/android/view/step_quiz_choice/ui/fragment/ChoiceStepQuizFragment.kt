package org.stepik.android.view.step_quiz_choice.ui.fragment

import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.layout_step_quiz_choice.*
import org.stepic.droid.R
import org.stepik.android.presentation.step_quiz.StepQuizFeature
import org.stepik.android.view.step_quiz.ui.delegate.StepQuizFormDelegate
import org.stepik.android.view.step_quiz.ui.fragment.DefaultStepQuizFragment
import org.stepik.android.view.step_quiz_choice.ui.delegate.ChoiceStepQuizFormDelegate
import ru.nobird.app.presentation.redux.container.ReduxView

class ChoiceStepQuizFragment :
    DefaultStepQuizFragment(),
    ReduxView<StepQuizFeature.State, StepQuizFeature.Action.ViewAction> {
    companion object {
        fun newInstance(stepId: Long): Fragment =
            ChoiceStepQuizFragment()
                .apply {
                    this.stepId = stepId
                }
    }

    override val quizLayoutRes: Int =
        R.layout.layout_step_quiz_choice

    override val quizViews: Array<View>
        get() = arrayOf(choicesRecycler)

    override fun createStepQuizFormDelegate(view: View): StepQuizFormDelegate =
        ChoiceStepQuizFormDelegate(view, onQuizChanged = ::syncReplyState)
}