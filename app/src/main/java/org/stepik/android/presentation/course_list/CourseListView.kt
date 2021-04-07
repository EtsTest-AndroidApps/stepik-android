package org.stepik.android.presentation.course_list

import ru.nobird.android.core.model.PagedList
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.presentation.course_continue.CourseContinueView

interface CourseListView : CourseContinueView {
    sealed class State {
        object Idle : State()
        object Loading : State()
        object Empty : State()
        object NetworkError : State()

        data class Content(
            val courseListDataItems: PagedList<CourseListItem.Data>,
            val courseListItems: List<CourseListItem>
        ) : State()
    }

    fun setState(state: State)
    fun showNetworkError()
}