package org.stepik.android.domain.course_list.interactor

import io.reactivex.Flowable
import io.reactivex.Single
import ru.nobird.android.core.model.PagedList
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course.model.SourceTypeComposition
import org.stepik.android.domain.course_list.model.CourseListItem
import org.stepik.android.domain.visited_courses.model.VisitedCourse
import org.stepik.android.domain.visited_courses.repository.VisitedCoursesRepository
import javax.inject.Inject

class CourseListVisitedInteractor
@Inject
constructor(
    private val visitedCoursesRepository: VisitedCoursesRepository,
    private val courseListInteractor: CourseListInteractor
) {
    fun getVisitedCourseListItems(): Flowable<PagedList<CourseListItem.Data>> =
        visitedCoursesRepository
            .observeVisitedCourses()
            .flatMapSingle { visitedCourses ->
                getCourseListItems(visitedCourses.map(VisitedCourse::course))
            }

    fun getCourseListItems(
        courseId: List<Long>,
        courseViewSource: CourseViewSource,
        sourceTypeComposition: SourceTypeComposition = SourceTypeComposition.REMOTE
    ): Single<PagedList<CourseListItem.Data>> =
        courseListInteractor.getCourseListItems(courseId, courseViewSource = courseViewSource, sourceTypeComposition = sourceTypeComposition)

    private fun getCourseListItems(courseIds: List<Long>): Single<PagedList<CourseListItem.Data>> =
        courseListInteractor
            .getCourseListItems(
                courseIds = courseIds,
                courseViewSource = CourseViewSource.Visited,
                sourceTypeComposition = SourceTypeComposition.CACHE
            )
}