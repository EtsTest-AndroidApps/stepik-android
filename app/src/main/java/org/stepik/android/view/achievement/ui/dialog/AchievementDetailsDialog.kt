package org.stepik.android.view.achievement.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_achievement_details.view.*
import org.stepic.droid.R
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepik.android.view.glide.ui.extension.wrapWithGlide
import org.stepik.android.domain.achievement.model.AchievementItem
import org.stepik.android.view.achievement.ui.resolver.AchievementResourceResolver
import ru.nobird.android.view.base.ui.extension.argument
import javax.inject.Inject

class AchievementDetailsDialog : DialogFragment() {
    companion object {
        const val TAG = "achievement_details_dialog"

        fun newInstance(achievementItem: AchievementItem, canShareAchievement: Boolean, source: String): AchievementDetailsDialog =
            AchievementDetailsDialog()
                .apply {
                    this.achievementItem = achievementItem
                    this.canShare = canShareAchievement
                    this.source = source
                }
    }

    @Inject
    lateinit var analytic: Analytic

    @Inject
    lateinit var achievementResourceResolver: AchievementResourceResolver

    private var achievementItem: AchievementItem by argument()
    private var canShare: Boolean by argument()
    private var source: String by argument()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component()
            .achievementsComponentBuilder()
            .build()
            .inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_achievement_details, null, false)

        view.apply {
            achievementTitle.text = achievementResourceResolver.resolveTitleForKind(achievementItem.kind)
            achievementDescription.text = achievementResourceResolver.resolveDescription(achievementItem)
            achievementIcon.apply {
                wrapWithGlide()
                    .setImagePath(
                        achievementResourceResolver.resolveAchievementIcon(achievementItem, resources.getDimensionPixelSize(R.dimen.achievement_details_icon_size)),
                        placeholder = AppCompatResources.getDrawable(context, R.drawable.ic_achievement_empty)
                    )
            }

            achievementLevelProgress.progress = achievementItem.currentScore.toFloat() / achievementItem.targetScore
            achievementLevels.progress = achievementItem.currentLevel
            achievementLevels.total = achievementItem.maxLevel

            achievementLevel.text = getString(R.string.achievement_level, achievementItem.currentLevel, achievementItem.maxLevel)

            val scoreDiff = achievementItem.targetScore - achievementItem.currentScore
            achievementRest.text = if (achievementItem.isLocked) {
                getString(R.string.achievement_remaining_exp_locked)
            } else {
                getString(R.string.achievement_remaining_exp, scoreDiff)
            }
            achievementRest.isVisible = scoreDiff > 0
        }

        val builder = MaterialAlertDialogBuilder(context)
                .setView(view)

        if (canShare && !achievementItem.isLocked) {
            builder
                .setPositiveButton(R.string.share_title) { _, _ ->
                    shareAchievement()
                }
                .setNegativeButton(R.string.close_screen, null)
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.attributes = dialog?.window?.attributes?.apply {
            width = resources.getDimension(R.dimen.achievement_details_dialog_width).toInt()
        }
    }

    private fun shareAchievement() {
        analytic.reportAmplitudeEvent(
            AmplitudeAnalytic.Achievements.SHARE_PRESSED,
            mapOf(
                AmplitudeAnalytic.Achievements.Params.SOURCE to source,
                AmplitudeAnalytic.Achievements.Params.KIND to achievementItem.kind,
                AmplitudeAnalytic.Achievements.Params.LEVEL to achievementItem.currentLevel
            )
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.achievement_share, achievementResourceResolver.resolveTitleForKind(achievementItem.kind)))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "text/plain"
        }

        activity?.startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
    }
}