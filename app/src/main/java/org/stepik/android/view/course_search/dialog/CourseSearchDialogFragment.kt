package org.stepik.android.view.course_search.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import org.stepic.droid.R
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.core.presenters.SearchSuggestionsPresenter
import org.stepic.droid.core.presenters.contracts.SearchSuggestionsView
import org.stepic.droid.databinding.DialogCourseSearchBinding
import org.stepic.droid.model.SearchQuery
import org.stepic.droid.model.SearchQuerySource
import org.stepic.droid.ui.custom.AutoCompleteSearchView
import org.stepic.droid.ui.dialogs.LoadingProgressDialogFragment
import org.stepic.droid.util.ProgressHelper
import org.stepik.android.domain.course_search.model.CourseSearchResultListItem
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.presentation.course_search.CourseSearchFeature
import org.stepik.android.presentation.course_search.CourseSearchViewModel
import org.stepik.android.view.course_search.adapter.delegate.CourseSearchResultAdapterDelegate
import org.stepik.android.view.course_search.adapter.delegate.CourseSearchResultLoadingAdapterDelegate
import org.stepik.android.view.lesson.ui.activity.LessonActivity
import ru.nobird.android.core.model.PaginationDirection
import ru.nobird.android.presentation.redux.container.ReduxView
import ru.nobird.android.ui.adapters.DefaultDelegateAdapter
import ru.nobird.android.view.base.ui.delegate.ViewStateDelegate
import ru.nobird.android.view.base.ui.extension.argument
import ru.nobird.android.view.base.ui.extension.setOnPaginationListener
import ru.nobird.android.view.redux.ui.extension.reduxViewModel
import javax.inject.Inject

class CourseSearchDialogFragment :
    DialogFragment(),
    ReduxView<CourseSearchFeature.State, CourseSearchFeature.Action.ViewAction>,
    SearchSuggestionsView,
    AutoCompleteSearchView.FocusCallback {

    companion object {
        const val TAG = "CourseSearchDialogFragment"

        fun newInstance(courseId: Long): DialogFragment =
            CourseSearchDialogFragment().apply {
                this.courseId = courseId
            }
    }

    @Inject
    lateinit var screenManager: ScreenManager

    @Inject
    lateinit var searchSuggestionsPresenter: SearchSuggestionsPresenter

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val progressDialogFragment: DialogFragment =
        LoadingProgressDialogFragment.newInstance()

    private val courseSearchViewModel: CourseSearchViewModel by reduxViewModel(this) { viewModelFactory }

    private val viewStateDelegate = ViewStateDelegate<CourseSearchFeature.State>()

    private val courseSearchResultItemsAdapter = DefaultDelegateAdapter<CourseSearchResultListItem>()

    private var courseId: Long by argument()

    private lateinit var searchIcon: ImageView

    private val courseSearchBinding: DialogCourseSearchBinding by viewBinding(DialogCourseSearchBinding::bind)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent()
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_AppTheme_Dialog_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_course_search, container, false)

    private fun injectComponent() {
        App.component()
            .courseSearchComponentBuilder()
            .courseId(courseId)
            .build()
            .inject(this)
    }

    private fun initViewStateDelegate() {
        viewStateDelegate.addState<CourseSearchFeature.State.Idle>(courseSearchBinding.courseSearchIdle.courseSearchIdleContainer)
        viewStateDelegate.addState<CourseSearchFeature.State.Error>(courseSearchBinding.courseSearchError.error)
        viewStateDelegate.addState<CourseSearchFeature.State.Loading>(courseSearchBinding.courseSearchRecycler)
        viewStateDelegate.addState<CourseSearchFeature.State.Empty>(courseSearchBinding.courseSearchEmpty.reportProblem)
        viewStateDelegate.addState<CourseSearchFeature.State.Content>(courseSearchBinding.courseSearchRecycler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchIcon = courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.findViewById(androidx.appcompat.R.id.search_mag_icon) as ImageView
        setupSearchBar()
        initViewStateDelegate()

        courseSearchResultItemsAdapter += CourseSearchResultLoadingAdapterDelegate()
        courseSearchResultItemsAdapter += CourseSearchResultAdapterDelegate(
            onOpenStepAction = { lesson, unit, section, stepPosition ->
                val lessonData = LessonData(
                    lesson = lesson,
                    unit = unit,
                    section = section,
                    course = null,
                    stepPosition = stepPosition ?: 0
                )
                val intent = LessonActivity.createIntent(requireContext(), lessonData)
                startActivity(intent)
            },
            onOpenCommentAction = { step, discussionId ->
                courseSearchViewModel.onNewMessage(CourseSearchFeature.Message.InitDiscussionThreadMessage(step, discussionId))
            }
        )

        with(courseSearchBinding.courseSearchRecycler) {
            adapter = courseSearchResultItemsAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                AppCompatResources.getDrawable(context, R.drawable.bg_divider_vertical_course_search)?.let(::setDrawable)
            })
            setOnPaginationListener { paginationDirection ->
                if (paginationDirection == PaginationDirection.NEXT) {
                    courseSearchViewModel.onNewMessage(CourseSearchFeature.Message.FetchNextPage(courseId, courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.query.toString()))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        searchSuggestionsPresenter.attachView(this)
    }

    override fun onStop() {
        searchSuggestionsPresenter.detachView(this)
        super.onStop()
    }

    override fun onAction(action: CourseSearchFeature.Action.ViewAction) {
        when (action) {
            is CourseSearchFeature.Action.ViewAction.ShowLoadingDialog -> {
                ProgressHelper.activate(progressDialogFragment, childFragmentManager, LoadingProgressDialogFragment.TAG)
            }
            is CourseSearchFeature.Action.ViewAction.HideLoadingDialog -> {
                ProgressHelper.dismiss(childFragmentManager, LoadingProgressDialogFragment.TAG)
            }
            is CourseSearchFeature.Action.ViewAction.OpenComment -> {
                screenManager.openComments(requireActivity(), action.discussionThread, action.step, action.discussionId, false, false)
            }
        }
    }

    override fun render(state: CourseSearchFeature.State) {
        viewStateDelegate.switchState(state)
        if (state is CourseSearchFeature.State.Loading) {
            courseSearchResultItemsAdapter.items = listOf(
                CourseSearchResultListItem.Placeholder,
                CourseSearchResultListItem.Placeholder,
                CourseSearchResultListItem.Placeholder,
                CourseSearchResultListItem.Placeholder,
                CourseSearchResultListItem.Placeholder
            )
        }
        if (state is CourseSearchFeature.State.Content) {
            courseSearchResultItemsAdapter.items =
                if (state.isLoadingNextPage) {
                    state.courseSearchResultListDataItems + CourseSearchResultListItem.Placeholder
                } else {
                    state.courseSearchResultListDataItems
                }
        }
    }

    override fun setSuggestions(suggestions: List<SearchQuery>, source: SearchQuerySource) {
        courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.setSuggestions(suggestions, source)
    }

    override fun onFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            searchSuggestionsPresenter.onQueryTextChange(courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.query.toString())
        }
    }

    private fun setupSearchBar() {
        courseSearchBinding.viewSearchToolbarBinding.viewCenteredToolbarBinding.centeredToolbar.isVisible = false
        courseSearchBinding.viewSearchToolbarBinding.backIcon.isVisible = true
        courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.isVisible = true
        searchIcon.setImageResource(0)
        (courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 0, 0)
        setupSearchView(courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar)
        courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.setFocusCallback(this)
        courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.setIconifiedByDefault(false)
        courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.setBackgroundColor(0)
        courseSearchBinding.viewSearchToolbarBinding.backIcon.setOnClickListener {
            val hasFocus = courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.hasFocus()
            if (hasFocus) {
                courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.clearFocus()
            } else {
                dismiss()
            }
        }
    }

    private fun setupSearchView(searchView: AutoCompleteSearchView) {
        searchView.initSuggestions(courseSearchBinding.courseSearchContainer)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchSuggestionsPresenter.onQueryTextSubmit(query)
                with(courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar) {
                    onActionViewCollapsed()
                    onActionViewExpanded()
                    clearFocus()
                    setQuery(query, false)
                }
                courseSearchViewModel.onNewMessage(CourseSearchFeature.Message.FetchCourseSearchResultsInitial(courseId, query))
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                courseSearchBinding.viewSearchToolbarBinding.searchViewToolbar.setConstraint(query)
                searchSuggestionsPresenter.onQueryTextChange(query)
                return false
            }
        })
        searchView.onActionViewExpanded()
        searchView.clearFocus()
    }
}