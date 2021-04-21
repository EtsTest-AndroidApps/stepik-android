package org.stepik.android.view.course_content.ui.adapter.delegates.unit

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.collection.LongSparseArray
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.view_course_content_unit.view.*
import org.stepic.droid.R
import org.stepic.droid.persistence.model.DownloadProgress
import org.stepic.droid.util.toFixed
import org.stepik.android.view.course_content.model.CourseContentItem
import ru.nobird.android.ui.adapterdelegates.AdapterDelegate
import ru.nobird.android.ui.adapterdelegates.DelegateViewHolder
import kotlin.math.abs

class CourseContentUnitDelegate(
    private val unitClickListener: CourseContentUnitClickListener,
    private val unitDownloadStatuses: LongSparseArray<DownloadProgress.Status>
) : AdapterDelegate<CourseContentItem, DelegateViewHolder<CourseContentItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
        ViewHolder(createView(parent, R.layout.view_course_content_unit))

    override fun isForViewType(position: Int, data: CourseContentItem): Boolean =
        data is CourseContentItem.UnitItem

    inner class ViewHolder(root: View) : DelegateViewHolder<CourseContentItem>(root) {
        private val unitIcon = root.unitIcon
        private val unitTitle = root.unitTitle
        private val unitDemoAccess = root.unitDemoAccess
        private val unitTextProgress = root.unitTextProgress
        private val unitProgress = root.unitProgress

        private val unitViewCount = root.unitViewCount
        private val unitViewCountIcon = root.unitViewCountIcon
        private val unitRating = root.unitRating
        private val unitRatingIcon = root.unitRatingIcon

        private val unitTimeToComplete = root.unitTimeToComplete

        private val unitDownloadStatus = root.unitDownloadStatus

        init {
            root.setOnClickListener {
                (itemData as? CourseContentItem.UnitItem)?.let(unitClickListener::onItemClicked)
            }

            unitDownloadStatus.setOnClickListener {
                val item = (itemData as? CourseContentItem.UnitItem) ?: return@setOnClickListener
                when (unitDownloadStatus.status) {
                    DownloadProgress.Status.NotCached ->
                        unitClickListener.onItemDownloadClicked(item)

                    is DownloadProgress.Status.InProgress ->
                        unitClickListener.onItemCancelClicked(item)

                    is DownloadProgress.Status.Cached ->
                        unitClickListener.onItemRemoveClicked(item)
                }
            }
        }

        override fun onBind(data: CourseContentItem) {
            with(data as CourseContentItem.UnitItem) {
                unitTitle.text = context.resources.getString(R.string.course_content_unit_title,
                        section.position, unit.position, lesson.title)
                if (progress != null && progress.cost > 0) {
                    val score = progress
                        .score
                        ?.toFloatOrNull()
                        ?: 0f

                    unitTextProgress.text = context.resources.getString(R.string.course_content_text_progress_points,
                        score.toFixed(context.resources.getInteger(R.integer.score_decimal_count)), progress.cost)

                    unitProgress.progress = score / progress.cost.toFloat()
                    unitTextProgress.isVisible = true
                } else {
                    unitProgress.progress = 0f
                    unitTextProgress.isVisible = false
                }

                val timeToComplete = lesson.timeToComplete.takeIf { it > 60 } ?: lesson.steps.size * 60L

                if (timeToComplete > 0) {
                    unitTimeToComplete.isVisible = true

                    val timeToCompleteString = if (timeToComplete in 0 until 3600) {
                        val timeValue = timeToComplete / 60
                        context.resources.getQuantityString(R.plurals.min, timeValue.toInt(), timeValue)
                    } else {
                        context.resources.getString(R.string.course_content_time_to_complete_hours_unit, timeToComplete / 3600)
                    }

                    unitTimeToComplete.text = context.getString(R.string.course_content_time_to_complete, timeToCompleteString)
                } else {
                    unitTimeToComplete.isVisible = false
                }

                unitDownloadStatus.status = unitDownloadStatuses[data.unit.id] ?: DownloadProgress.Status.Pending

                Glide.with(unitIcon.context)
                    .asBitmap()
                    .load(lesson.coverUrl)
                    .placeholder(R.drawable.general_placeholder)
                    .centerCrop()
                    .into(unitIcon)

                unitViewCount.text = lesson.passedBy.toString()

                @DrawableRes
                val unitRatingDrawableRes =
                    if (lesson.voteDelta < 0) {
                        R.drawable.ic_course_content_dislike
                    } else {
                        R.drawable.ic_course_content_like
                    }

                unitRatingIcon.setImageResource(unitRatingDrawableRes)
                unitRating.text = abs(lesson.voteDelta).toString()

                unitDownloadStatus.isVisible = access == CourseContentItem.UnitItem.Access.FULL_ACCESS
                unitDemoAccess.isVisible = access == CourseContentItem.UnitItem.Access.DEMO
                itemView.isEnabled = access != CourseContentItem.UnitItem.Access.NO_ACCESS

                val alpha = if (access != CourseContentItem.UnitItem.Access.NO_ACCESS) 1f else 0.4f
                unitTitle.alpha = alpha
                unitRatingIcon.alpha = alpha
                unitRating.alpha = alpha
                unitViewCount.alpha = alpha
                unitViewCountIcon.alpha = alpha
                unitTimeToComplete.alpha = alpha
                unitTextProgress.alpha = alpha
            }
        }
    }
}