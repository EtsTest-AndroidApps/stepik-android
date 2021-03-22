package org.stepik.android.view.course.routing

import android.net.Uri
import org.stepic.droid.configuration.Config
import org.stepik.android.view.base.routing.ExternalDeepLinkProcessor
import ru.nobird.android.view.base.ui.extension.appendQueryParameters
import javax.inject.Inject

class CourseDeepLinkBuilder
@Inject
constructor(
    private val config: Config,
    private val externalDeepLinkProcessor: ExternalDeepLinkProcessor
) {
    fun createCourseLink(courseId: Long, tab: CourseScreenTab, queryParams: Map<String, List<String>>? = null): String =
        Uri.parse("${config.baseUrl}/$COURSE_PATH_SEGMENT/$courseId/${tab.path}")
            .buildUpon()
            .appendQueryParameters(queryParams ?: emptyMap())
            .let(externalDeepLinkProcessor::processExternalDeepLink)
            .build()
            .toString()
}