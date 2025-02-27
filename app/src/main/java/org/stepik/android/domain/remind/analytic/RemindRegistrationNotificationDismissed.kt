package org.stepik.android.domain.remind.analytic

import kotlinx.android.parcel.Parcelize
import org.stepik.android.domain.base.analytic.ParcelableAnalyticEvent

@Parcelize
object RemindRegistrationNotificationDismissed : ParcelableAnalyticEvent {
    override val name: String =
        "Remind registration notification dismissed"
}