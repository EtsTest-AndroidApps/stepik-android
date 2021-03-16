package org.stepik.android.remote.stories.service

import org.stepik.android.remote.stories.model.StoryTemplatesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StoryService {
    @GET("api/story-templates")
    suspend fun getStoryTemplate(@Query("ids[]") storyIds: List<Long>): StoryTemplatesResponse

    @GET("api/story-templates?platform=mobile,android")
    suspend fun getStoryTemplate(
        @Query("page") page: Int,
        @Query("is_published") isPublished: Boolean,
        @Query("language") language: String
    ): StoryTemplatesResponse
}