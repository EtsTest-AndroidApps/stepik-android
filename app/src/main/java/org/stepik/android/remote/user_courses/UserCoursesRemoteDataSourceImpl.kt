package org.stepik.android.remote.user_courses

import io.reactivex.Single
import ru.nobird.app.core.model.PagedList
import ru.nobird.android.domain.rx.first
import org.stepik.android.data.user_courses.source.UserCoursesRemoteDataSource
import org.stepik.android.domain.course_list.model.UserCourseQuery
import org.stepik.android.domain.user_courses.model.UserCourse
import org.stepik.android.remote.base.mapper.toPagedList
import org.stepik.android.remote.user_courses.mapper.UserCourseQueryMapper
import org.stepik.android.remote.user_courses.model.UserCoursesRequest
import org.stepik.android.remote.user_courses.model.UserCoursesResponse
import org.stepik.android.remote.user_courses.service.UserCoursesService
import javax.inject.Inject

class UserCoursesRemoteDataSourceImpl
@Inject
constructor(
    private val userCoursesService: UserCoursesService,
    private val userCourseQueryMapper: UserCourseQueryMapper
) : UserCoursesRemoteDataSource {
    override fun getUserCourses(userCourseQuery: UserCourseQuery): Single<PagedList<UserCourse>> =
        userCoursesService
            .getUserCourses(userCourseQueryMapper.mapToQueryMap(userCourseQuery))
            .map { it.toPagedList(UserCoursesResponse::userCourse) }

    override fun saveUserCourse(userCourseId: Long, userCourse: UserCourse): Single<UserCourse> =
        userCoursesService
            .saveUserCourse(userCourseId, UserCoursesRequest(userCourse))
            .map(UserCoursesResponse::userCourse)
            .first()
}