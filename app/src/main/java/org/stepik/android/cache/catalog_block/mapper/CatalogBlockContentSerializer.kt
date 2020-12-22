package org.stepik.android.cache.catalog_block.mapper

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.stepik.android.domain.catalog_block.model.CatalogAuthor
import org.stepik.android.domain.catalog_block.model.CatalogBlockContent
import org.stepik.android.domain.catalog_block.model.CatalogCourseList

class CatalogBlockContentSerializer {
    private val gson = Gson()
    private val jsonParser = JsonParser()

    fun mapToLocalEntity(kind: String, content: CatalogBlockContent?): String {
        val memberName = when (content) {
            is CatalogBlockContent.FullCourseList ->
                "courseList"

            is CatalogBlockContent.SimpleCourseLists ->
                "courseLists"

            is CatalogBlockContent.AuthorsList ->
                "authors"

            else ->
                null
        }

        val contentField = gson.toJsonTree(content).asJsonObject[memberName]

        val contentJson = if (contentField is JsonObject) {
            JsonArray(1).apply { add(contentField) }
        } else {
            contentField
        }

        val localEntity = JsonObject()
        localEntity.addProperty("kind", kind)
        localEntity.add("content", contentJson)
        return localEntity.toString()
    }

    fun mapToDomainEntity(value: String?): CatalogBlockContent {
        val parsed = jsonParser.parse(value) as JsonObject
        val kind = parsed.remove("kind").asString
        val contentField = parsed["content"]
        return when (kind) {
            CatalogBlockContent.FULL_COURSE_LISTS ->
                CatalogBlockContent.FullCourseList(gson.fromJson<ArrayList<CatalogCourseList>>(contentField, TypeToken.getParameterized(ArrayList::class.java, CatalogCourseList::class.java).type).first())

            CatalogBlockContent.SIMPLE_COURSE_LISTS ->
                CatalogBlockContent.SimpleCourseLists(gson.fromJson(contentField, TypeToken.getParameterized(ArrayList::class.java, CatalogCourseList::class.java).type))

            CatalogBlockContent.AUTHORS ->
                CatalogBlockContent.AuthorsList(gson.fromJson(contentField, TypeToken.getParameterized(ArrayList::class.java, CatalogAuthor::class.java).type))

            else ->
                CatalogBlockContent.Unsupported
        }
    }
}