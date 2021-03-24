package org.stepik.android.view.fast_continue.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_fast_continue.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.ui.activities.MainFeedActivity
import org.stepic.droid.ui.dialogs.LoadingProgressDialogFragment
import org.stepic.droid.util.ProgressHelper
import org.stepic.droid.util.toFixed
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.domain.last_step.model.LastStep
import org.stepik.android.model.Course
import org.stepik.android.presentation.course_continue.model.CourseContinueInteractionSource
import org.stepik.android.presentation.fast_continue.FastContinuePresenter
import org.stepik.android.presentation.fast_continue.FastContinueView
import org.stepik.android.view.ui.delegate.ViewStateDelegate
import javax.inject.Inject

class FastContinueFragment : Fragment(R.layout.fragment_fast_continue), FastContinueView {
    companion object {
        fun newInstance(): FastContinueFragment =
            FastContinueFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var analytic: Analytic

    @Inject
    internal lateinit var screenManager: ScreenManager

    private val fastContinuePresenter: FastContinuePresenter by viewModels { viewModelFactory }
    private lateinit var viewStateDelegate: ViewStateDelegate<FastContinueView.State>

    private val progressDialogFragment: DialogFragment =
        LoadingProgressDialogFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent()
    }

    private fun injectComponent() {
        App
            .component()
            .fastContinueComponentBuilder()
            .build()
            .inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewStateDelegate = ViewStateDelegate()
        viewStateDelegate.addState<FastContinueView.State.Idle>()
        viewStateDelegate.addState<FastContinueView.State.Loading>(fastContinueProgress)
        viewStateDelegate.addState<FastContinueView.State.Empty>(fastContinuePlaceholder)
        viewStateDelegate.addState<FastContinueView.State.Anonymous>(fastContinuePlaceholder)
        viewStateDelegate.addState<FastContinueView.State.Content>(fastContinueMask)

        fastContinueOverlay.isEnabled = true
        fastContinueAction.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        fastContinuePresenter.attachView(this)
    }

    override fun onStop() {
        fastContinuePresenter.detachView(this)
        super.onStop()
    }

    override fun setState(state: FastContinueView.State) {
        viewStateDelegate.switchState(state)
        when (state) {
            is FastContinueView.State.Empty -> {
                analytic.reportEvent(Analytic.FastContinue.EMPTY_COURSES_SHOWN)
                showPlaceholder(R.string.placeholder_explore_courses) {
                    analytic.reportEvent(Analytic.FastContinue.EMPTY_COURSES_CLICK)
                    screenManager.showCatalog(context)
                }
            }
            is FastContinueView.State.Anonymous -> {
                analytic.reportEvent(Analytic.FastContinue.AUTH_SHOWN)
                showPlaceholder(R.string.placeholder_login) {
                    analytic.reportEvent(Analytic.FastContinue.AUTH_CLICK)
                    screenManager.showLaunchScreen(context, true, MainFeedActivity.HOME_INDEX)
                }
            }
            is FastContinueView.State.Content -> {
                analytic.reportEvent(Analytic.FastContinue.CONTINUE_SHOWN)
                setCourse(state.courseListItem)
                fastContinueOverlay.setOnClickListener { handleContinueCourseClick(state.courseListItem.course) }
                fastContinueAction.setOnClickListener { handleContinueCourseClick(state.courseListItem.course) }
            }
        }
    }

    private fun setCourse(courseListItem: CourseListItem.Data) {
        Glide
            .with(requireContext())
            .asBitmap()
            .load(courseListItem.course.cover)
            .placeholder(R.drawable.general_placeholder)
            .fitCenter()
            .into(fastContinueCourseCover)

        fastContinueCourseName.text = courseListItem.course.title

        val progress = courseListItem.courseStats.progress
        val needShow = if (progress != null && progress.cost > 0f) {
            val score = progress
                .score
                ?.toFloatOrNull()
                ?: 0f

            fastContinueCourseProgressText.text = getString(R.string.course_current_progress, score.toFixed(resources.getInteger(R.integer.score_decimal_count)), progress.cost)
            fastContinueCourseProgress.progress = (score * 100 / progress.cost).toInt()
            true
        } else {
            fastContinueCourseProgress.progress = 0
            false
        }
        fastContinueCourseProgressText.isVisible = needShow
    }

    private fun showPlaceholder(@StringRes stringRes: Int, listener: (view: View) -> Unit) {
        fastContinuePlaceholder.setPlaceholderText(stringRes)
        fastContinuePlaceholder.setOnClickListener(listener)
    }

    private fun handleContinueCourseClick(course: Course) {
        analytic.reportEvent(Analytic.FastContinue.CONTINUE_CLICK)
        fastContinuePresenter.continueCourse(course, CourseViewSource.FastContinue, CourseContinueInteractionSource.HOME_WIDGET)
    }

    override fun showCourse(course: Course, source: CourseViewSource, isAdaptive: Boolean) {
        if (isAdaptive) {
            screenManager.continueAdaptiveCourse(activity, course)
        } else {
            screenManager.showCourseModules(activity, course, source)
        }
    }

    override fun showSteps(course: Course, source: CourseViewSource, lastStep: LastStep) {
        screenManager.continueCourse(activity, course.id, source, lastStep)
    }

    override fun setBlockingLoading(isLoading: Boolean) {
        fastContinueOverlay.isEnabled = !isLoading
        fastContinueAction.isEnabled = !isLoading
        if (isLoading) {
            ProgressHelper.activate(progressDialogFragment, fragmentManager, LoadingProgressDialogFragment.TAG)
        } else {
            ProgressHelper.dismiss(fragmentManager, LoadingProgressDialogFragment.TAG)
        }
    }
}
