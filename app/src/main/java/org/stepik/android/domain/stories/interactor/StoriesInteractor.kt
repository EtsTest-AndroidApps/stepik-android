package org.stepik.android.domain.stories.interactor

import android.content.res.Resources
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.stepic.droid.features.stories.mapper.toStory
import org.stepic.droid.features.stories.repository.StoryTemplatesRepository
import org.stepic.droid.util.defaultLocale
import org.stepik.android.domain.personal_offers.repository.PersonalOffersRepository
import org.stepik.android.model.StoryTemplate
import ru.nobird.android.stories.model.Story
import javax.inject.Inject

class StoriesInteractor
@Inject
constructor(
    private val personalOffersRepository: PersonalOffersRepository,
    private val storiesRepository: StoryTemplatesRepository
) {
    fun fetchStories(): Single<List<Story>> =
        Singles.zip(
            getStoryTemplates(),
            getOfferStoryTemplates()
        ) { stories, offerStories ->
            (stories + offerStories)
                .sortedBy { it.position }
                .map(StoryTemplate::toStory)
        }

    fun getViewedStoriesIds(): Single<Set<Long>> =
        storiesRepository.getViewedStoriesIds()

    fun markStoryAsViewed(storyId: Long): Completable =
        storiesRepository.markStoryAsViewed(storyId)

    private fun getStoryTemplates(): Single<List<StoryTemplate>> {
        val locale = Resources.getSystem().configuration.defaultLocale
        return storiesRepository.getStoryTemplates(locale.language)
    }

    private fun getOfferStoryTemplates(): Single<List<StoryTemplate>> =
        personalOffersRepository.getPersonalOffers()
            .flatMap { offersWrapper ->
                storiesRepository.getStoryTemplates(offersWrapper.data.promoStories.orEmpty())
            }
}