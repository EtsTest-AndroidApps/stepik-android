package org.stepik.android.model

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.stepik.android.model.util.assertThatObjectParcelable

@RunWith(RobolectricTestRunner::class)
class ProgressTest {

    @Test
    fun progressIsParcelable() {
        val progress = Progress(id = "76-2222", score = "227", cost = 337)
        progress.assertThatObjectParcelable<Progress>()
    }
}
