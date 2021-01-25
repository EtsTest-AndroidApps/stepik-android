package org.stepik.android.data.stories.source

import io.reactivex.Single
import org.stepik.android.model.StoryTemplate

interface StoryTemplatesRemoteDataSource {
    fun getStoryTemplates(ids: List<Long>): Single<List<StoryTemplate>>
    fun getStoryTemplates(lang: String): Single<List<StoryTemplate>>
}