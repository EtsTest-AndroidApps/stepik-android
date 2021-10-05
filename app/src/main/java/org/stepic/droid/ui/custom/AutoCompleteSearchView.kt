package org.stepic.droid.ui.custom

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.stepic.droid.R
import org.stepic.droid.model.SearchQuery
import org.stepic.droid.model.SearchQuerySource
import org.stepic.droid.ui.adapters.SearchQueriesAdapter
import org.stepic.droid.util.resolveResourceIdAttribute

class AutoCompleteSearchView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : SearchView(context, attrs, defStyleAttr) {
    private val searchQueriesAdapter = SearchQueriesAdapter(context)
    private val closeIcon: ImageView = findViewById(androidx.appcompat.R.id.search_close_btn)
    private val searchIcon: ImageView = findViewById(androidx.appcompat.R.id.search_mag_icon)

    private var focusCallback: FocusCallback? = null

    private val colorControlNormal =
        AppCompatResources.getColorStateList(context, context.resolveResourceIdAttribute(R.attr.colorControlNormal))

    var suggestionsOnTouchListener: OnTouchListener? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        searchQueriesAdapter.searchView = this
    }

    override fun onDetachedFromWindow() {
        searchQueriesAdapter.searchView = null
        focusCallback = null
        searchQueriesAdapter.suggestionClickCallback = null
        super.onDetachedFromWindow()
    }

    init {
        maxWidth = 20000
        ImageViewCompat.setImageTintList(closeIcon, colorControlNormal)
        ImageViewCompat.setImageTintList(searchIcon, colorControlNormal)
    }

    fun initSuggestions(rootView: ViewGroup) {
        val inflater = LayoutInflater.from(context)
        val searchQueriesRecyclerView = inflater.inflate(R.layout.search_queries_recycler_view, rootView, false) as RecyclerView
        searchQueriesRecyclerView.layoutManager = LinearLayoutManager(context)
        searchQueriesRecyclerView.adapter = searchQueriesAdapter

        searchQueriesRecyclerView.setOnTouchListener { v, event ->
            if (searchQueriesRecyclerView.findChildViewUnder(event.x, event.y) == null) {
                if (event.action == MotionEvent.ACTION_UP && isEventInsideView(v, event)) {
                    this@AutoCompleteSearchView.clearFocus()
                }
            } else {
                suggestionsOnTouchListener?.onTouch(v, event)
            }
            false
        }

        setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setConstraint(query.toString())
                searchQueriesRecyclerView.layoutManager?.scrollToPosition(0)
                searchQueriesRecyclerView.visibility = View.VISIBLE

            } else {
                searchQueriesRecyclerView.visibility = View.GONE
            }
            focusCallback?.onFocusChanged(hasFocus)
        }

        rootView.addView(searchQueriesRecyclerView)

    }

    fun setFocusCallback(focusCallback: FocusCallback) {
        this.focusCallback = focusCallback
    }

    fun setSuggestionClickCallback(suggestionClickCallback: SuggestionClickCallback) {
        searchQueriesAdapter.suggestionClickCallback = suggestionClickCallback
    }

    fun setCloseIconDrawableRes(@DrawableRes iconRes: Int) {
        closeIcon.setImageResource(iconRes)
    }

    fun setSearchable(activity: Activity) {
        val searchManager = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val componentName = activity.componentName
        val searchableInfo = searchManager.getSearchableInfo(componentName)
        setSearchableInfo(searchableInfo)
    }

    fun setConstraint(constraint: String) {
        searchQueriesAdapter.constraint = constraint
    }

    fun setSuggestions(suggestions: List<SearchQuery>, source: SearchQuerySource) {
        when (source) {
            SearchQuerySource.API ->
                searchQueriesAdapter.rawAPIItems = suggestions
            SearchQuerySource.DB ->
                searchQueriesAdapter.rawDBItems = suggestions
        }
    }

    private fun isEventInsideView(v: View, event: MotionEvent): Boolean =
        event.x > 0 && event.y > 0 &&
        event.x < v.width && event.y < v.height

    interface FocusCallback {
        fun onFocusChanged(hasFocus: Boolean)
    }

    interface SuggestionClickCallback {
        fun onQueryTextSubmitSuggestion(query: String)
    }
}