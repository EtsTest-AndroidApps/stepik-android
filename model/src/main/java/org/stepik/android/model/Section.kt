package org.stepik.android.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import ru.nobird.android.core.model.Identifiable
import java.util.Date

@Parcelize
data class Section(
    @SerializedName("id")
    override val id: Long = 0,
    @SerializedName("course")
    val course: Long = 0, // course id
    @SerializedName("units")
    val units: List<Long> = emptyList(),
    @SerializedName("position")
    val position: Int = 0,
    @SerializedName("progress")
    override val progress: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("slug")
    val slug: String? = null,

    @SerializedName("begin_date")
    val beginDate: Date? = null,
    @SerializedName("end_date")
    val endDate: Date? = null,
    @SerializedName("soft_deadline")
    val softDeadline: Date? = null,
    @SerializedName("hard_deadline")
    val hardDeadline: Date? = null,

    @SerializedName("create_date")
    val createDate: Date? = null,
    @SerializedName("update_date")
    val updateDate: Date? = null,

    @SerializedName("grading_policy")
    val gradingPolicy: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = false,

    @SerializedName("actions")
    val actions: Actions? = null,
    @SerializedName("is_exam")
    val isExam: Boolean = false,
    @SerializedName("exam_duration_minutes")
    val examDurationMinutes: Int? = null,
    @SerializedName("exam_session")
    val examSession: Long? = null,
    @SerializedName("proctor_session")
    val proctorSession: Long? = null,
    @SerializedName("is_proctoring_can_be_scheduled")
    val isProctoringCanBeScheduled: Boolean = false,
    @SerializedName("discounting_policy")
    val discountingPolicy: DiscountingPolicyType? = null,
    @SerializedName("is_requirement_satisfied")
    val isRequirementSatisfied: Boolean = true,
    @SerializedName("required_section")
    val requiredSection: Long = 0, //id of required section
    @SerializedName("required_percent")
    val requiredPercent: Int = 0
) : Parcelable, Progressable, Identifiable<Long>