package org.stepic.droid.features.stories.repository

import io.reactivex.Completable
import io.reactivex.Single
import org.stepik.android.model.StoryTemplate

interface StoryTemplatesRepository {
    fun getStoryTemplate(id: Long): Single<StoryTemplate>

    fun getStoryTemplates(ids: List<Long>): Single<List<StoryTemplate>>

    fun getStoryTemplates(lang: String): Single<List<StoryTemplate>>

    fun getViewedStoriesIds(): Single<Set<Long>>

    fun markStoryAsViewed(storyTemplateId: Long): Completable
}