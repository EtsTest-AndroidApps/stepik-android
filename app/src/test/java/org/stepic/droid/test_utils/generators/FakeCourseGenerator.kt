package org.stepic.droid.test_utils.generators

import org.stepic.droid.model.Course

object FakeCourseGenerator {
    @JvmOverloads
    fun generate(courseId: Long = 0,
                 sectionIds: LongArray? = null): Course {
        val course = Course()
        course.setId(courseId)
        course.sections = sectionIds
        return course
    }
}