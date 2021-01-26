package org.stepik.android.presentation.stories.dispatcher

import io.reactivex.Scheduler
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepik.android.domain.stories.interactor.StoriesInteractor
import org.stepik.android.presentation.stories.StoriesFeature
import ru.nobird.android.domain.rx.emptyOnErrorStub
import ru.nobird.android.presentation.redux.dispatcher.RxActionDispatcher
import javax.inject.Inject

class StoriesActionDispatcher
@Inject
constructor(
    private val storiesInteractor: StoriesInteractor,

    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler
) : RxActionDispatcher<StoriesFeature.Action, StoriesFeature.Message>() {
    override fun handleAction(action: StoriesFeature.Action) {
        when (action) {
            is StoriesFeature.Action.FetchStories ->
                compositeDisposable += Singles
                    .zip(
                        storiesInteractor.fetchStories(),
                        storiesInteractor.getViewedStoriesIds()
                    ) { stories, viewedIds ->
                        StoriesFeature.Message.FetchStoriesSuccess(
                            stories.sortedBy { if (it.id in viewedIds) 1 else 0 },
                            viewedIds
                        )
                    }
                    .subscribeOn(backgroundScheduler)
                    .observeOn(mainScheduler)
                    .subscribeBy(
                        onSuccess = { onNewMessage(it) },
                        onError = { onNewMessage(StoriesFeature.Message.FetchStoriesError) }
                    )

            is StoriesFeature.Action.MarkStoryAsViewed ->
                compositeDisposable += storiesInteractor
                    .markStoryAsViewed(action.storyId)
                    .subscribeBy(
                        onComplete = {},
                        onError = emptyOnErrorStub
                    )
        }
    }
}