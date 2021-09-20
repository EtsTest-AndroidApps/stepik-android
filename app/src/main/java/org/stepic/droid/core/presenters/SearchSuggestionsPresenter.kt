package org.stepic.droid.core.presenters

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import org.stepic.droid.analytic.AmplitudeAnalytic
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.core.presenters.contracts.SearchSuggestionsView
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.CourseId
import org.stepic.droid.di.qualifiers.MainScheduler
import org.stepic.droid.model.SearchQuerySource
import org.stepic.droid.storage.operations.DatabaseFacade
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.search.repository.SearchRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchSuggestionsPresenter
@Inject
constructor(
    @CourseId
    private val courseId: Long,
    private val searchRepository: SearchRepository,
    private val databaseFacade: DatabaseFacade,
    private val analytic: Analytic,
    @BackgroundScheduler
    private val scheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler
) : PresenterBase<SearchSuggestionsView>() {

    companion object {
        private const val AUTOCOMPLETE_DEBOUNCE_MS = 300L
        private const val DB_ELEMENTS_COUNT = 2
    }

    private val compositeDisposable = CompositeDisposable()
    private val publisher = PublishSubject.create<String>()

    override fun attachView(view: SearchSuggestionsView) {
        super.attachView(view)
        initSearchView(view)
    }

    private fun initSearchView(searchView: SearchSuggestionsView) {
        val queryPublisher = publisher
            .debounce(AUTOCOMPLETE_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
            .subscribeOn(scheduler)

        compositeDisposable += queryPublisher
            .flatMap { query -> searchRepository.getSearchQueries(courseId, query, DataSourceType.CACHE).toObservable().onErrorResumeNext(Observable.empty()) }
            .observeOn(mainScheduler)
            .subscribe { searchView.setSuggestions(it, SearchQuerySource.DB) }

        compositeDisposable += queryPublisher
                .flatMap { query -> searchRepository.getSearchQueries(courseId, query, DataSourceType.REMOTE).toObservable().onErrorResumeNext(Observable.empty()) }
                .observeOn(mainScheduler)
                .subscribe { searchView.setSuggestions(it, SearchQuerySource.API) }
    }

    fun onQueryTextChange(query: String) {
        publisher.onNext(query)
    }

    fun onQueryTextSubmit(query: String) {
        analytic.reportEventWithName(Analytic.Search.SEARCH_SUBMITTED, query)
        analytic.reportAmplitudeEvent(AmplitudeAnalytic.Search.SEARCHED, mapOf(AmplitudeAnalytic.Search.PARAM_SUGGESTION to query.toLowerCase()))
    }

    override fun detachView(view: SearchSuggestionsView) {
        compositeDisposable.clear()
        super.detachView(view)
    }
}