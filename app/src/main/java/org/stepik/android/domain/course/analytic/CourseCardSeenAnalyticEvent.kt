package org.stepik.android.domain.course.analytic

import org.stepik.android.domain.base.analytic.AnalyticEvent

class CourseCardSeenAnalyticEvent(
    courseId: Long,
    source: CourseViewSource
) : AnalyticEvent {
    companion object {
        private const val PARAM_COURSE = "course"
        private const val PARAM_SOURCE = "source"
    }

    override val name: String =
        "Course card seen"

    override val params: Map<String, Any> =
        mapOf(
            PARAM_COURSE to courseId,
            PARAM_SOURCE to source.name
        ) + source.params.mapKeys { "${PARAM_SOURCE}_$it" }
}