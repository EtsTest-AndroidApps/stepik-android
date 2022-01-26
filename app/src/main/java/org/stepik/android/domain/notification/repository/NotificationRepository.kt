package org.stepik.android.domain.notification.repository

import io.reactivex.Completable
import io.reactivex.Single
import org.stepic.droid.model.NotificationCategory
import org.stepic.droid.notifications.model.Notification
import org.stepic.droid.notifications.model.NotificationStatuses
import ru.nobird.app.core.model.PagedList

interface NotificationRepository {
    fun putNotifications(vararg notificationIds: Long, isRead: Boolean): Completable
    fun getNotificationsByCourseId(courseId: Long): Single<List<Notification>>
    fun getNotifications(notificationCategory: NotificationCategory, page: Int): Single<PagedList<Notification>>
    fun markNotificationAsRead(notificationCategory: NotificationCategory): Completable
    fun getNotificationStatuses(): Single<List<NotificationStatuses>>
}