package org.stepik.android.domain.debug.model

import ru.nobird.android.core.model.Identifiable

data class SplitGroupData(
    val splitTestName: String,
    val splitTestValue: String,
    val splitTestGroups: List<String>
) : Identifiable<String> {
    override val id: String =
        splitTestName
}