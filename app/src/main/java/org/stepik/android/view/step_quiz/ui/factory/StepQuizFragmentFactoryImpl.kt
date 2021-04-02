package org.stepik.android.view.step_quiz.ui.factory

import androidx.fragment.app.Fragment
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.stepic.droid.configuration.RemoteConfig
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepic.droid.util.AppConstants
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.view.step_quiz_choice.ui.fragment.ChoiceStepQuizFragment
import org.stepik.android.view.step_quiz_code.ui.fragment.CodeStepQuizFragment
import org.stepik.android.view.step_quiz_fill_blanks.ui.fragment.FillBlanksStepQuizFragment
import org.stepik.android.view.step_quiz_matching.ui.fragment.MatchingStepQuizFragment
import org.stepik.android.view.step_quiz_sorting.ui.fragment.SortingStepQuizFragment
import org.stepik.android.view.step_quiz_sql.ui.fragment.SqlStepQuizFragment
import org.stepik.android.view.step_quiz_text.ui.fragment.TextStepQuizFragment
import org.stepik.android.view.step_quiz_unsupported.ui.fragment.UnsupportedStepQuizFragment
import org.stepik.android.view.step_quiz_pycharm.ui.fragment.PyCharmStepQuizFragment
import org.stepik.android.view.step_quiz_review.ui.fragment.StepQuizReviewFragment
import org.stepik.android.view.step_quiz_review.ui.fragment.StepQuizReviewTeacherFragment
import org.stepik.android.view.step_quiz_table.ui.fragment.TableStepQuizFragment
import javax.inject.Inject

class StepQuizFragmentFactoryImpl
@Inject
constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : StepQuizFragmentFactory {
    override fun createStepQuizFragment(stepPersistentWrapper: StepPersistentWrapper, lessonData: LessonData): Fragment {
        val instructionType =
            stepPersistentWrapper.step.instructionType.takeIf { stepPersistentWrapper.step.actions?.doReview != null }

        val blockName = stepPersistentWrapper.step.block?.name

        return if (instructionType != null && firebaseRemoteConfig.getBoolean(RemoteConfig.IS_PEER_REVIEW_ENABLED)) {
            when {
                lessonData.lesson.isTeacher && blockName in StepQuizReviewTeacherFragment.supportedQuizTypes ->
                    StepQuizReviewTeacherFragment.newInstance(stepPersistentWrapper.step.id, instructionType)

                !lessonData.lesson.isTeacher && blockName in StepQuizReviewFragment.supportedQuizTypes ->
                    StepQuizReviewFragment.newInstance(stepPersistentWrapper.step.id, instructionType)

                else ->
                    getDefaultQuizFragment(stepPersistentWrapper)
            }
        } else {
            getDefaultQuizFragment(stepPersistentWrapper)
        }
    }

    private fun getDefaultQuizFragment(stepPersistentWrapper: StepPersistentWrapper): Fragment =
        when (stepPersistentWrapper.step.block?.name) {
            AppConstants.TYPE_STRING,
            AppConstants.TYPE_NUMBER,
            AppConstants.TYPE_MATH,
            AppConstants.TYPE_FREE_ANSWER ->
                TextStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_CHOICE ->
                ChoiceStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_CODE ->
                CodeStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_SORTING ->
                SortingStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_MATCHING ->
                MatchingStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_PYCHARM ->
                PyCharmStepQuizFragment.newInstance()

            AppConstants.TYPE_SQL ->
                SqlStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_FILL_BLANKS ->
                FillBlanksStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            AppConstants.TYPE_TABLE ->
                TableStepQuizFragment.newInstance(stepPersistentWrapper.step.id)

            else ->
                UnsupportedStepQuizFragment.newInstance(stepPersistentWrapper.step.id)
        }
}