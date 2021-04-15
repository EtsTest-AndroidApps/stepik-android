package org.stepik.android.presentation.profile_courses

import ru.nobird.android.core.model.PagedList
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.presentation.course_continue.CourseContinueView

interface ProfileCoursesView : CourseContinueView {
    sealed class State {
        object Idle : State()
        object Loading : State()
        object Empty : State()
        object Error : State()

        class Content(val courseListDataItems: PagedList<CourseListItem.Data>) : State()
    }

    fun setState(state: State)
}