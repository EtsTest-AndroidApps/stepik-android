package org.stepik.android.presentation.catalog

import org.stepik.android.presentation.catalog.model.OldCatalogItem
import org.stepik.android.presentation.course_list.CourseListCollectionPresenter

interface CatalogView {
    data class State(
        val headers: List<OldCatalogItem>,
        val collectionsState: CollectionsState,
        val footers: List<OldCatalogItem>
    )

    sealed class CollectionsState {
        object Idle : CollectionsState()
        object Loading : CollectionsState()
        object Error : CollectionsState()
        class Content(val collections: List<CourseListCollectionPresenter>) : CollectionsState()
    }

    fun setState(state: State)
}