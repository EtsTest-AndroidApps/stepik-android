package org.stepik.android.view.step_content_video.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_step_content_video.*
import kotlinx.android.synthetic.main.view_course_info_video.*
import kotlinx.android.synthetic.main.view_length_video_thumbnail.*
import org.stepic.droid.R
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.core.ScreenManager
import org.stepic.droid.persistence.model.StepPersistentWrapper
import org.stepic.droid.ui.util.snackbar
import org.stepik.android.domain.lesson.model.LessonData
import org.stepik.android.presentation.step_content_video.VideoStepContentPresenter
import org.stepik.android.presentation.step_content_video.VideoStepContentView
import org.stepik.android.view.lesson.ui.activity.LessonActivity
import org.stepik.android.view.lesson.ui.interfaces.Playable
import org.stepik.android.view.video_player.model.VideoPlayerMediaData
import ru.nobird.android.view.base.ui.extension.argument
import javax.inject.Inject

class VideoStepContentFragment : Fragment(), VideoStepContentView, Playable {
    companion object {
        fun newInstance(stepId: Long): Fragment =
            VideoStepContentFragment()
                .apply {
                    this.stepId = stepId
                }
    }

    @Inject
    internal lateinit var analytic: Analytic

    @Inject
    internal lateinit var screenManager: ScreenManager

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var stepWrapper: StepPersistentWrapper

    @Inject
    internal lateinit var lessonData: LessonData

    private val presenter: VideoStepContentPresenter by viewModels { viewModelFactory }
    private var stepId: Long by argument()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectComponent()

        if (savedInstanceState == null) {
            presenter.fetchVideoLength(stepWrapper)
        }
    }

    private fun injectComponent() {
        App.componentManager()
            .stepComponent(stepId)
            .videoStepContentComponentBuilder()
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_step_content_video, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val thumbnail = stepWrapper.cachedVideo?.thumbnail
            ?: stepWrapper.step.block?.video?.thumbnail

        Glide.with(this)
            .load(thumbnail)
            .placeholder(R.drawable.general_placeholder)
            .into(videoThumbnail)

        videoContainer.setOnClickListener { openVideoPlayer() }
    }

    private fun openVideoPlayer() {
        if (stepWrapper.cachedVideo == null && stepWrapper.step.block?.video == null) {
            analytic.reportEventWithName(Analytic.Error.ILLEGAL_STATE_VIDEO_STEP_PLAY, stepWrapper.step.id.toString())
            videoStepContent.snackbar(messageRes = R.string.step_content_video_no_video)
        } else {
            val thumbnail = stepWrapper.cachedVideo?.thumbnail
                ?: stepWrapper.step.block?.video?.thumbnail

            val lessonMovementBundle = Bundle()
                lessonMovementBundle.putAll(requireActivity().intent.extras)
                lessonMovementBundle.putInt(LessonActivity.EXTRA_AUTOPLAY_STEP_POSITION, lessonData.lesson.steps.indexOfFirst { it == stepWrapper.step.id })
                lessonMovementBundle.putBoolean(LessonActivity.EXTRA_AUTOPLAY_MOVE_NEXT, true)

            screenManager.showVideo(this, VideoPlayerMediaData(
                thumbnail = thumbnail,
                title = lessonData.lesson.title.orEmpty(),
                cachedVideo = stepWrapper.cachedVideo,
                externalVideo = stepWrapper.step.block?.video
            ), lessonMovementBundle)
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
    }

    override fun onStop() {
        presenter.detachView(this)
        super.onStop()
    }

    override fun setState(state: VideoStepContentView.State) {
        val videoLengthText = (state as? VideoStepContentView.State.Loaded)
            ?.videoLength

        videoLength.isVisible = videoLengthText != null
        videoLength.text = videoLengthText
    }

    override fun play(): Boolean {
        openVideoPlayer()
        return true
    }
}