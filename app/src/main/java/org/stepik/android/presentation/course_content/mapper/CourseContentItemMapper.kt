package org.stepik.android.presentation.course_content.mapper

import org.stepic.droid.util.hasUserAccess
import org.stepik.android.domain.exam_session.model.ExamSession
import org.stepik.android.domain.proctor_session.model.ProctorSession
import org.stepik.android.model.Course
import org.stepik.android.model.Lesson
import org.stepik.android.model.Progress
import org.stepik.android.model.Section
import org.stepik.android.model.Unit
import org.stepik.android.view.course_content.model.CourseContentItem
import org.stepik.android.view.course_content.model.RequiredSection
import timber.log.Timber
import javax.inject.Inject

class CourseContentItemMapper
@Inject
constructor(
    private val sectionDatesMapper: CourseContentSectionDatesMapper
) {
    fun mapSectionsWithEmptyUnits(course: Course, sections: List<Section>, unitItems: List<CourseContentItem.UnitItem>, progresses: List<Progress>, sessions: List<Pair<ExamSession, ProctorSession>>): List<CourseContentItem> =
        sections
            .flatMap { section ->
                Timber.d("Sessions: $sessions")
                val (examSession, proctorSession) = sessions.find { it.first.section == section.id || it.second.section == section.id } ?: null to null
                mapSectionWithEmptyUnits(unitItems, course, section, progresses, sections, examSession, proctorSession)
            }

    private fun mapSectionWithEmptyUnits(unitItems: List<CourseContentItem.UnitItem>, course: Course, section: Section, progresses: List<Progress>, sections: List<Section>, examSession: ExamSession?, proctorSession: ProctorSession?): List<CourseContentItem> =
        listOf(CourseContentItem.SectionItem(
            section = section,
            dates = sectionDatesMapper.mapSectionDates(section),
            progress = progresses.find { it.id == section.progress },
            isEnabled = section.hasUserAccess(course),
            requiredSection = mapRequiredSection(section, sections, progresses),
            examSession = examSession,
            proctorSession = proctorSession
        )) + mapSectionUnits(section.units, unitItems)

    private fun mapSectionUnits(unitIds: List<Long>, unitItems: List<CourseContentItem.UnitItem>): List<CourseContentItem> =
        unitIds
            .map { unitId ->
                unitItems
                    .find { it.unit.id == unitId }
                    ?: CourseContentItem.UnitItemPlaceholder(unitId)
            }

    private fun mapRequiredSection(section: Section, sections: List<Section>, progresses: List<Progress>): RequiredSection? =
        if (!section.isRequirementSatisfied) {
            val requiredSection = sections.find { it.id == section.requiredSection }
            val progress = progresses.find { it.id == requiredSection?.progress }

            if (requiredSection != null && progress != null) {
                RequiredSection(requiredSection, progress)
            } else {
                null
            }
        } else {
            null
        }

    fun mapUnits(course: Course, sectionItems: List<CourseContentItem.SectionItem>, units: List<Unit>, lessons: List<Lesson>, progresses: List<Progress>): List<CourseContentItem.UnitItem> =
        units.mapNotNull { unit ->
            val sectionItem = sectionItems.find { it.section.id == unit.section } ?: return@mapNotNull null
            val lesson = lessons.find { it.id == unit.lesson } ?: return@mapNotNull null
            val progress = progresses.find { it.id == unit.progress }
            CourseContentItem.UnitItem(
                sectionItem.section, unit, lesson, progress,
                access =
                    if (course.enrollment == 0L && course.isPaid && lesson.actions?.learnLesson != null) {
                        CourseContentItem.UnitItem.Access.DEMO
                    } else {
                        if (sectionItem.isEnabled) {
                            CourseContentItem.UnitItem.Access.FULL_ACCESS
                        } else {
                            CourseContentItem.UnitItem.Access.NO_ACCESS
                        }
                    }
            )
        }

    fun replaceUnits(items: List<CourseContentItem>, unitItems: List<CourseContentItem.UnitItem>, progresses: List<Progress>): List<CourseContentItem> =
        items.map { item ->
            when (item) {
                is CourseContentItem.UnitItem ->
                    unitItems
                        .find { unitItem -> item.unit.id == unitItem.unit.id  }
                        ?: item.copy(progress = progresses.find { it.id == item.unit.progress } ?: item.progress)

                is CourseContentItem.UnitItemPlaceholder ->
                    unitItems
                        .find { unitItem -> item.unitId == unitItem.unit.id }
                        ?: item

                else ->
                    item
            }
        }

    fun getUnitPlaceholdersIds(items: List<CourseContentItem>): List<Long> =
        items
            .filterIsInstance<CourseContentItem.SectionItem>()
            .fold(emptyList()) { acc, sectionItem -> acc + sectionItem.section.units }
}