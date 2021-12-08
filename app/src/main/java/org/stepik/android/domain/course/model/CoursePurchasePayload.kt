package org.stepik.android.domain.course.model

import com.google.gson.annotations.SerializedName

data class CoursePurchasePayload(
    @SerializedName("profile_id")
    val profileId: Long,
    @SerializedName("course_id")
    val courseId: Long
)