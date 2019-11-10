package org.stepik.android.data.course.repository

import io.reactivex.Maybe
import io.reactivex.Single
import org.stepic.droid.util.doCompletableOnSuccess
import org.stepic.droid.util.maybeFirst
import org.stepic.droid.util.requireSize
import org.stepik.android.data.course.source.CourseCacheDataSource
import org.stepik.android.data.course.source.CourseRemoteDataSource
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.course.repository.CourseRepository
import org.stepik.android.model.Course
import javax.inject.Inject

class CourseRepositoryImpl
@Inject
constructor(
    private val courseRemoteDataSource: CourseRemoteDataSource,
    private val courseCacheDataSource: CourseCacheDataSource
) : CourseRepository {

    override fun getCourse(courseId: Long, canUseCache: Boolean): Maybe<Course> {
        val remoteSource = courseRemoteDataSource.getCoursesReactive(courseId).maybeFirst()
            .doCompletableOnSuccess(courseCacheDataSource::saveCourse)

        val cacheSource = courseCacheDataSource.getCourses(courseId).maybeFirst()

        return if (canUseCache) {
            cacheSource.switchIfEmpty(remoteSource)
        } else {
            remoteSource
        }
    }

    override fun getCourses(vararg courseIds: Long, primarySourceType: DataSourceType): Single<List<Course>> {
        val remoteSource = courseRemoteDataSource
            .getCoursesReactive(*courseIds)
            .doCompletableOnSuccess(courseCacheDataSource::saveCourses)

        val cacheSource = courseCacheDataSource
            .getCourses(*courseIds)

        return when (primarySourceType) {
            DataSourceType.REMOTE ->
                remoteSource.onErrorResumeNext(cacheSource.requireSize(courseIds.size))

            DataSourceType.CACHE ->
                cacheSource.flatMap { cachedCourses ->
                    val ids = (courseIds.toList() - cachedCourses.map(Course::id)).toLongArray()
                    courseRemoteDataSource
                        .getCoursesReactive(*ids)
                        .doCompletableOnSuccess(courseCacheDataSource::saveCourses)
                        .map { remoteCourses -> cachedCourses + remoteCourses }
                }

            else ->
                throw IllegalArgumentException("Unsupported source type = $primarySourceType")
        }.map { courses -> courses.sortedBy { courseIds.indexOf(it.id) } }
    }
}