package org.stepik.android.presentation.course_list_redux.model

import org.stepik.android.domain.catalog_block.model.CatalogBlockContent.Companion.AUTHORS
import org.stepik.android.domain.catalog_block.model.CatalogBlockContent.Companion.FULL_COURSE_LISTS
import org.stepik.android.domain.catalog_block.model.CatalogBlockItem
import org.stepik.android.presentation.author_list.AuthorListFeature
import org.stepik.android.presentation.course_list_redux.CourseListFeature
import ru.nobird.android.core.model.Identifiable

sealed class CatalogBlockStateWrapper : Identifiable<String> {
    data class CourseList(val catalogBlockItem: CatalogBlockItem, val state: CourseListFeature.State) : CatalogBlockStateWrapper() {
        override val id: String = "$FULL_COURSE_LISTS${catalogBlockItem.id}"
    }

    data class AuthorList(val catalogBlockItem: CatalogBlockItem, val state: AuthorListFeature.State) : CatalogBlockStateWrapper() {
        override val id: String = "${AUTHORS}${catalogBlockItem.id}"
    }
}