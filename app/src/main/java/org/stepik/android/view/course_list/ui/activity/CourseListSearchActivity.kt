package org.stepik.android.view.course_list.ui.activity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import org.stepic.droid.R
import org.stepic.droid.base.FragmentActivityBase
import org.stepik.android.domain.filter.model.CourseListFilterQuery
import org.stepik.android.view.course_list.ui.fragment.CourseListSearchFragment
import org.stepik.android.view.filter.ui.dialog.FilterBottomSheetDialogFragment

class CourseListSearchActivity : FragmentActivityBase(), FilterBottomSheetDialogFragment.Callback  {
    private var query: String? = null
    private var filterQuery: CourseListFilterQuery? = null

    companion object {
        const val EXTRA_COURSE_LIST_FILTER_QUERY = "course_list_filter_query"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.search_title)
        setContentView(R.layout.activity_search_courses)
        query = intent.getStringExtra(SearchManager.QUERY)
        filterQuery = intent.getParcelableExtra(EXTRA_COURSE_LIST_FILTER_QUERY) ?: CourseListFilterQuery(language = sharedPreferenceHelper.languageForFeatured)
        initOrTryRestoreFragment()
    }

    private fun initOrTryRestoreFragment() {
        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(R.id.frame)
        if (fragment == null) {
            fragment = createFragment()
            fm.beginTransaction()
                .add(R.id.frame, fragment)
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent) // add new fragment
        setIntent(intent)
        query = intent.getStringExtra(SearchManager.QUERY)
        updateFragment()
    }

    private fun updateFragment() {
        val fm = supportFragmentManager
        val fragment: Fragment = createFragment()
        fm.beginTransaction()
            .replace(R.id.frame, fragment)
            .commit()
    }

    private fun createFragment(): Fragment =
        CourseListSearchFragment.newInstance(query, filterQuery)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Respond to the action bar's Up/Home button
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSyncFilterQueryWithParent(filterQuery: CourseListFilterQuery) {
        this.query = intent.getStringExtra(SearchManager.QUERY)
        this.filterQuery = filterQuery
        updateFragment()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_start, R.anim.slide_out_to_end)
    }
}