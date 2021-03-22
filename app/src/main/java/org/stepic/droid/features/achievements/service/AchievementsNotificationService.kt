package org.stepic.droid.features.achievements.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.stepic.droid.R
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepik.android.view.achievement.ui.activity.AchievementsListActivity
import org.stepik.android.view.achievement.ui.resolver.AchievementResourceResolver
import org.stepik.android.domain.achievement.model.AchievementItem
import org.stepic.droid.model.AchievementNotification
import org.stepic.droid.notifications.model.StepikNotificationChannel
import org.stepik.android.view.glide.model.GlideRequestFactory
import org.stepic.droid.util.resolveColorAttribute
import org.stepic.droid.util.toObject
import org.stepik.android.domain.achievement.repository.AchievementRepository
import javax.inject.Inject

class AchievementsNotificationService : JobIntentService() {
    companion object {
        private const val NOTIFICATION_TAG = "achievement"

        private const val EXTRA_RAW_MESSAGE = "raw_message"
        private const val JOB_ID = 2002

        fun enqueueWork(context: Context, rawMessage: String?) {
            enqueueWork(context, AchievementsNotificationService::class.java, JOB_ID, Intent().putExtra(EXTRA_RAW_MESSAGE, rawMessage))
        }
    }

    @Inject
    internal lateinit var analytic: Analytic

    @Inject
    internal lateinit var achievementRepository: AchievementRepository

    @Inject
    internal lateinit var notificationManager: NotificationManager

    @Inject
    internal lateinit var achievementResourceResolver: AchievementResourceResolver

    init {
        App.component().inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        try {
            val rawMessage = intent.getStringExtra(EXTRA_RAW_MESSAGE) ?: return
            val achievementNotification = rawMessage.toObject<AchievementNotification>()

            val achievement = achievementRepository
                    .getAchievement(achievementNotification.user, achievementNotification.kind)
                    .blockingGet()

            analytic.reportAmplitudeEvent(
                AmplitudeAnalytic.Achievements.NOTIFICATION_RECEIVED,
                mapOf(
                    AmplitudeAnalytic.Achievements.Params.KIND to achievement.kind,
                    AmplitudeAnalytic.Achievements.Params.LEVEL to achievement.currentLevel
                )
            )

            val notificationIntent = AchievementsListActivity
                    .createIntent(this, achievementNotification.user, isMyProfile = true)

            val pendingIntent = PendingIntent
                    .getActivity(this, 0, notificationIntent, 0)

            val largeIcon = getAchievementImageBitmap(achievement)

            val notification = NotificationCompat.Builder(this, StepikNotificationChannel.user.channelId)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.achievement_notification_message,
                            achievementResourceResolver.resolveTitleForKind(achievement.kind)))
                    .setSmallIcon(R.drawable.ic_notification_icon_1)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(pendingIntent)
                    .setColor(resolveColorAttribute(R.attr.colorSecondary))
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()

            notificationManager.notify(NOTIFICATION_TAG, achievementNotification.achievement, notification)
        } catch (e: Exception) {}
    }

    private fun getAchievementImageBitmap(achievement: AchievementItem): Bitmap {
        val iconSize = resources.getDimension(R.dimen.notification_large_icon_size).toInt()
        return GlideRequestFactory.create(this, null)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .load(Uri.parse(achievementResourceResolver.resolveAchievementIcon(achievement, iconSize)))
                .placeholder(R.drawable.ic_achievement_empty)
                .submit(iconSize, iconSize)
                .get()
                .toBitmap(iconSize, iconSize)
    }

}