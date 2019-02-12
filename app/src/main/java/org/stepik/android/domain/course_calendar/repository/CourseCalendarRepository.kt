package org.stepik.android.domain.course_calendar.repository

import io.reactivex.Completable
import io.reactivex.Single
import org.stepik.android.domain.course_calendar.model.SectionDateEvent

interface CourseCalendarRepository {
    fun getSectionDateEvents(): Single<List<SectionDateEvent>>
    fun saveSectionDateEvents(events: List<SectionDateEvent>): Completable
}