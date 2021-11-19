package org.stepik.android.presentation.course

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.solovyev.android.checkout.UiCheckout
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.di.qualifiers.BackgroundScheduler
import org.stepic.droid.di.qualifiers.CourseId
import org.stepic.droid.di.qualifiers.MainScheduler
import ru.nobird.android.domain.rx.emptyOnErrorStub
import org.stepic.droid.util.plus
import org.stepik.android.domain.course.analytic.CoursePreviewScreenOpenedAnalyticEvent
import org.stepik.android.domain.course.analytic.CourseViewSource
import org.stepik.android.domain.course.analytic.UserCourseActionEvent
import org.stepik.android.domain.course.analytic.batch.CoursePreviewScreenOpenedAnalyticBatchEvent
import org.stepik.android.domain.course.interactor.CourseBillingInteractor
import org.stepik.android.domain.course.interactor.CourseEnrollmentInteractor
import org.stepik.android.domain.course.interactor.CourseIndexingInteractor
import org.stepik.android.domain.course.interactor.CourseInteractor
import org.stepik.android.domain.course.mapper.CourseStateMapper
import org.stepik.android.domain.course.model.CourseHeaderData
import org.stepik.android.domain.course.model.EnrollmentState
import org.stepik.android.domain.notification.interactor.CourseNotificationInteractor
import org.stepik.android.domain.purchase_notification.interactor.PurchaseReminderInteractor
import org.stepik.android.domain.solutions.interactor.SolutionsInteractor
import org.stepik.android.domain.solutions.model.SolutionItem
import org.stepik.android.domain.user_courses.interactor.UserCoursesInteractor
import org.stepik.android.domain.user_courses.model.UserCourse
import org.stepik.android.domain.visited_courses.interactor.VisitedCoursesInteractor
import org.stepik.android.domain.wishlist.analytic.CourseWishlistAddedEvent
import org.stepik.android.domain.wishlist.analytic.CourseWishlistRemovedEvent
import org.stepik.android.domain.wishlist.interactor.WishlistInteractor
import org.stepik.android.domain.wishlist.model.WishlistOperationData
import org.stepik.android.model.Course
import org.stepik.android.presentation.course.mapper.toEnrollmentError
import org.stepik.android.presentation.course.model.EnrollmentError
import org.stepik.android.presentation.course_continue.delegate.CourseContinuePresenterDelegate
import org.stepik.android.presentation.course_continue.delegate.CourseContinuePresenterDelegateImpl
import org.stepik.android.presentation.course_continue.model.CourseContinueInteractionSource
import org.stepik.android.presentation.user_courses.model.UserCourseAction
import org.stepik.android.presentation.wishlist.model.WishlistAction
import org.stepik.android.view.injection.course.EnrollmentCourseUpdates
import org.stepik.android.view.injection.course_list.UserCoursesOperationBus
import org.stepik.android.view.injection.course_list.WishlistOperationBus
import org.stepik.android.view.injection.solutions.SolutionsBus
import org.stepik.android.view.injection.solutions.SolutionsSentBus
import ru.nobird.android.core.model.safeCast
import ru.nobird.android.presentation.base.PresenterBase
import ru.nobird.android.presentation.base.PresenterViewContainer
import ru.nobird.android.presentation.base.delegate.PresenterDelegate
import javax.inject.Inject

class CoursePresenter
@Inject
constructor(
    @CourseId
    private val courseId: Long,

    viewContainer: PresenterViewContainer<CourseView>,

    private val courseContinuePresenterDelegateImpl: CourseContinuePresenterDelegateImpl,

    private val courseStateMapper: CourseStateMapper,

    private val courseInteractor: CourseInteractor,
    private val courseBillingInteractor: CourseBillingInteractor,
    private val courseEnrollmentInteractor: CourseEnrollmentInteractor,
    private val courseIndexingInteractor: CourseIndexingInteractor,
    private val solutionsInteractor: SolutionsInteractor,
    private val userCoursesInteractor: UserCoursesInteractor,
    private val visitedCoursesInteractor: VisitedCoursesInteractor,
    private val wishlistInteractor: WishlistInteractor,

    private val courseNotificationInteractor: CourseNotificationInteractor,
    private val coursePurchaseReminderInteractor: PurchaseReminderInteractor,

    @EnrollmentCourseUpdates
    private val enrollmentUpdatesObservable: Observable<Course>,

    @SolutionsBus
    private val solutionsObservable: Observable<Unit>,

    @SolutionsSentBus
    private val solutionsSentObservable: Observable<Unit>,

    @UserCoursesOperationBus
    private val userCourseOperationObservable: Observable<UserCourse>,

    @WishlistOperationBus
    private val wishlistOperationObservable: Observable<WishlistOperationData>,

    @BackgroundScheduler
    private val backgroundScheduler: Scheduler,
    @MainScheduler
    private val mainScheduler: Scheduler,
    private val analytic: Analytic
) : PresenterBase<CourseView>(viewContainer), CourseContinuePresenterDelegate by courseContinuePresenterDelegateImpl {
    private var state: CourseView.State = CourseView.State.Idle
        set(value) {
            field = value
            view?.setState(value)
            startIndexing()
        }

    private var uiCheckout: UiCheckout? = null

    private var isCoursePreviewLogged = false
    private var isNeedCheckCourseEnrollment = false
    private lateinit var viewSource: CourseViewSource

    override val delegates: List<PresenterDelegate<in CourseView>> =
        listOf(courseContinuePresenterDelegateImpl)

    private val userCourseDisposable = CompositeDisposable()

    init {
        compositeDisposable += userCourseDisposable
        subscriberForEnrollmentUpdates()
        subscribeForLocalSubmissionsUpdates()
        subscribeForUserCoursesUpdates()
        subscribeForWishlistUpdates()
    }

    override fun attachView(view: CourseView) {
        super.attachView(view)
        view.setState(state)
        startIndexing()

        uiCheckout = view
            .createUiCheckout()
            .also(UiCheckout::start)
    }

    override fun detachView(view: CourseView) {
        super.detachView(view)
        endIndexing()

        uiCheckout?.let(UiCheckout::stop)
        uiCheckout = null
    }

    /**
     * Data initialization variants
     */
    fun onCourseId(courseId: Long, viewSource: CourseViewSource, promo: String? = null, forceUpdate: Boolean = false) {
        val courseHeaderDataSource = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.course
            ?.let { courseInteractor.getCourseHeaderData(it, canUseCache = !forceUpdate) }
            ?: courseInteractor.getCourseHeaderData(courseId, promo = promo, canUseCache = !forceUpdate)
        observeCourseData(courseHeaderDataSource, viewSource, forceUpdate)
    }

    fun onCourse(course: Course, viewSource: CourseViewSource, forceUpdate: Boolean = false) {
        val courseToPass = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.course
            ?: course
        observeCourseData(courseInteractor.getCourseHeaderData(courseToPass, canUseCache = !forceUpdate), viewSource, forceUpdate)
    }

    private fun observeCourseData(courseDataSource: Maybe<CourseHeaderData>, viewSource: CourseViewSource, forceUpdate: Boolean) {
        if (state != CourseView.State.Idle &&
            !((state == CourseView.State.NetworkError || state is CourseView.State.CourseLoaded) && forceUpdate)
        ) {
            return
        }

        this.viewSource = viewSource

        state = CourseView.State.Loading
        compositeDisposable += courseDataSource
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onComplete = { state = CourseView.State.EmptyCourse },
                onSuccess  = {
                    state = CourseView.State.CourseLoaded(it)
                    postCourseViewedNotification(it.courseId)
                    logCoursePreviewOpenedEvent(it.course, viewSource)
                    saveVisitedCourse(it.courseId)
                },
                onError    = { state = CourseView.State.NetworkError }
            )
    }

    private fun saveVisitedCourse(courseId: Long) {
        compositeDisposable += visitedCoursesInteractor
            .saveVisitedCourse(courseId)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(onError = emptyOnErrorStub)
    }

    private fun postCourseViewedNotification(courseId: Long) {
        compositeDisposable += courseNotificationInteractor
            .markCourseNotificationsAsRead(courseId)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(onError = emptyOnErrorStub)
    }

    /**
     * Enrollment
     */
    fun autoEnroll() {
        val enrollmentState = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.stats
            ?.enrollmentState
            ?: return

        when (enrollmentState) {
            EnrollmentState.NotEnrolledFree ->
                enrollCourse()

            EnrollmentState.NotEnrolledWeb ->
                openCoursePurchaseInWeb()

            is EnrollmentState.NotEnrolledInApp ->
                purchaseCourse()
        }
    }

    fun enrollCourse() {
        toggleEnrollment(CourseEnrollmentInteractor::enrollCourse)
    }

    fun dropCourse() {
        toggleEnrollment(CourseEnrollmentInteractor::dropCourse)
    }

    private inline fun toggleEnrollment(enrollmentAction: CourseEnrollmentInteractor.(Long) -> Single<Course>) {
        val headerData = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.takeIf { it.stats.enrollmentState != EnrollmentState.Pending }
            ?: return

        state = CourseView.State.BlockingLoading(
            headerData.copy(
                stats = headerData.stats.copy(
                    enrollmentState = EnrollmentState.Pending
                )
            )
        )

        userCourseDisposable.clear()

        compositeDisposable += courseEnrollmentInteractor
            .enrollmentAction(headerData.courseId)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onError = {
                    state = CourseView.State.CourseLoaded(headerData) // roll back data

                    val errorType = it.toEnrollmentError()
                    if (errorType == EnrollmentError.UNAUTHORIZED) {
                        view?.showEmptyAuthDialog(headerData.course)
                    } else {
                        view?.showEnrollmentError(errorType)
                    }
                }
            )
    }

    private fun subscriberForEnrollmentUpdates() {
        compositeDisposable += enrollmentUpdatesObservable
            .filter { it.id == courseId }
            .concatMap { courseInteractor.getCourseHeaderData(it).toObservable() }
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext  = { state = CourseView.State.CourseLoaded(it); continueLearning(); resolveCourseShareTooltip(it) },
                onError = { state = CourseView.State.NetworkError; subscriberForEnrollmentUpdates() }
            )
    }

    private fun subscribeForLocalSubmissionsUpdates() {
        compositeDisposable += (solutionsObservable + solutionsSentObservable)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext = { updateLocalSubmissionsCount() },
                onError = emptyOnErrorStub
            )
    }

    private fun updateLocalSubmissionsCount() {
        compositeDisposable += solutionsInteractor
            .fetchAttemptCacheItems(courseId, localOnly = true)
            .map { localSubmissions -> localSubmissions.count { it is SolutionItem.SubmissionItem } }
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onSuccess = { localSubmissionsCount ->
                    val oldState =
                        (state as? CourseView.State.CourseLoaded)
                        ?: return@subscribeBy

                    val courseHeaderData = oldState
                        .courseHeaderData
                        .copy(localSubmissionsCount = localSubmissionsCount)
                    state = CourseView.State.CourseLoaded(courseHeaderData)
                },
                onError = emptyOnErrorStub
            )
    }

    private fun resolveCourseShareTooltip(courseHeaderData: CourseHeaderData) {
        if (courseHeaderData.stats.enrollmentState is EnrollmentState.Enrolled) {
            view?.showCourseShareTooltip()
        }
    }

    /**
     * Purchases
     */
    fun restoreCoursePurchase() {
        val headerData = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?: return

        val sku = (headerData.stats.enrollmentState as? EnrollmentState.NotEnrolledInApp)
            ?.skuWrapper
            ?.sku
            ?: return

        state = CourseView.State.BlockingLoading(
            headerData.copy(
                stats = headerData.stats.copy(
                    enrollmentState = EnrollmentState.Pending
                )
            )
        )
        compositeDisposable += courseBillingInteractor
            .restorePurchase(sku)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onError = {
                    state = CourseView.State.CourseLoaded(headerData) // roll back data

                    val errorType = it.toEnrollmentError()
                    analytic.reportError(errorType.name, it)

                    when (errorType) {
                        EnrollmentError.UNAUTHORIZED ->
                            view?.showEmptyAuthDialog(headerData.course)

                        EnrollmentError.COURSE_ALREADY_OWNED ->
                            enrollCourse() // try to enroll course normally

                        else ->
                            view?.showEnrollmentError(errorType)
                    }
                }
            )
    }

    fun purchaseCourse() {
        val headerData = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?: return

        val sku = (headerData.stats.enrollmentState as? EnrollmentState.NotEnrolledInApp)
            ?.skuWrapper
            ?.sku
            ?: return

        val checkout = this.uiCheckout
            ?: return

        schedulePurchaseReminder()

        state = CourseView.State.BlockingLoading(
            headerData.copy(
                stats = headerData.stats.copy(enrollmentState = EnrollmentState.Pending)
            )
        )
        compositeDisposable += courseBillingInteractor
            .purchaseCourse(checkout, headerData.courseId, sku)
            .observeOn(mainScheduler)
            .subscribeOn(backgroundScheduler)
            .subscribeBy(
                onError = {
                    state = CourseView.State.CourseLoaded(headerData) // roll back data

                    val errorType = it.toEnrollmentError()
                    analytic.reportError(errorType.name, it)

                    if (errorType == EnrollmentError.UNAUTHORIZED) {
                        view?.showEmptyAuthDialog(headerData.course)
                    } else {
                        view?.showEnrollmentError(errorType)
                    }
                }
            )
    }

    fun handleCoursePurchasePressed() {
        if (!isNeedCheckCourseEnrollment) {
            return
        }

        isNeedCheckCourseEnrollment = false
        userCourseDisposable += courseEnrollmentInteractor
            .fetchCourseEnrollmentAfterPurchaseInWeb(courseId)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(onError = emptyOnErrorStub)
    }

    fun openCoursePurchaseInWeb(queryParams: Map<String, List<String>>? = null) {
        isNeedCheckCourseEnrollment = true
        schedulePurchaseReminder()
        view?.openCoursePurchaseInWeb(courseId, queryParams)
    }

    /**
     * Continue learning
     */
    fun continueLearning() {
        val headerData = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.takeIf { it.stats.enrollmentState is EnrollmentState.Enrolled }
            ?: return

        courseContinuePresenterDelegateImpl.continueCourse(headerData.course, viewSource, CourseContinueInteractionSource.COURSE_SCREEN)
    }

    fun tryLessonFree(lessonId: Long) {
        view?.showTrialLesson(lessonId)
    }

    /**
     * Indexing
     */
    private fun startIndexing() {
        (state as? CourseView.State.CourseLoaded)
            ?.takeIf { view != null }
            ?.courseHeaderData
            ?.course
            ?.let(courseIndexingInteractor::startIndexing)
    }

    private fun endIndexing() {
        courseIndexingInteractor.endIndexing()
    }

    /**
     * Sharing
     */
    fun shareCourse() {
        val course = (state as? CourseView.State.CourseLoaded)
            ?.courseHeaderData
            ?.course
            ?: return

        view?.shareCourse(course)
    }

    /**
     * User course operations
     */

    fun toggleUserCourse(userCourseAction: UserCourseAction) {
        val oldUserCourse = state.safeCast<CourseView.State.CourseLoaded>()
            ?.courseHeaderData
            ?.stats
            ?.enrollmentState
            ?.safeCast<EnrollmentState.Enrolled>()
            ?.userCourse
            ?: return

        val userCourse =
            when (userCourseAction) {
                UserCourseAction.ADD_ARCHIVE ->
                    oldUserCourse.copy(isArchived = true)

                UserCourseAction.REMOVE_ARCHIVE ->
                    oldUserCourse.copy(isArchived = false)

                UserCourseAction.ADD_FAVORITE ->
                    oldUserCourse.copy(isFavorite = true)

                UserCourseAction.REMOVE_FAVORITE ->
                    oldUserCourse.copy(isFavorite = false)
            }

        state = courseStateMapper.mutateEnrolledState(state) { copy(isUserCourseUpdating = true) }
        saveUserCourse(userCourse, userCourseAction)
    }

    private fun saveUserCourse(userCourse: UserCourse, userCourseAction: UserCourseAction) {
        userCourseDisposable += userCoursesInteractor
            .saveUserCourse(userCourse = userCourse)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onSuccess = {
                    logUserCourseAction(userCourseAction, viewSource)
                    view?.showSaveUserCourseSuccess(userCourseAction)
                },
                onError = {
                    state = courseStateMapper.mutateEnrolledState(state) { copy(isUserCourseUpdating = false) }
                    view?.showSaveUserCourseError(userCourseAction)
                }
            )
    }

    fun toggleWishlist(wishlistAction: WishlistAction) {
        val courseHeaderData = state.safeCast<CourseView.State.CourseLoaded>()
            ?.courseHeaderData
            ?: return

        state = CourseView.State.CourseLoaded(
            courseHeaderData = courseHeaderData.copy(
                isWishlistUpdating = true
            )
        )
        saveWishlistAction(wishlistAction)
    }

    private fun saveWishlistAction(wishlistAction: WishlistAction) {
        compositeDisposable += wishlistInteractor
            .updateWishlistWithOperation(WishlistOperationData(courseId, wishlistAction))
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onComplete = {
                    val oldState = state.safeCast<CourseView.State.CourseLoaded>()
                        ?: return@subscribeBy
                    val isWishlisted = wishlistAction == WishlistAction.ADD
                    state = CourseView.State.CourseLoaded(oldState.courseHeaderData.copy(stats = oldState.courseHeaderData.stats.copy(isWishlisted = isWishlisted)))
                    logWishlistAction(wishlistAction, viewSource)
                    view?.showWishlistActionSuccess(wishlistAction)
                },
                onError = {
                    val oldState = state.safeCast<CourseView.State.CourseLoaded>()
                        ?: return@subscribeBy
                    state = CourseView.State.CourseLoaded(oldState.courseHeaderData.copy(isWishlistUpdating = false))
                    view?.showWishlistActionFailure(wishlistAction)
                }
            )
    }

    private fun subscribeForUserCoursesUpdates() {
        compositeDisposable += userCourseOperationObservable
            .filter { it.course == courseId }
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext = { userCourse ->
                    state = courseStateMapper.mutateEnrolledState(state) { copy(userCourse = userCourse, isUserCourseUpdating = false) }
                },
                onError = emptyOnErrorStub
            )
    }

    private fun subscribeForWishlistUpdates() {
        compositeDisposable += wishlistOperationObservable
            .filter { it.courseId == courseId }
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onNext = { wishlistOperation ->
                    val oldState = state.safeCast<CourseView.State.CourseLoaded>()
                        ?: return@subscribeBy
                    val isWishlisted = wishlistOperation.wishlistAction == WishlistAction.ADD

                    state = CourseView.State.CourseLoaded(
                        courseHeaderData = oldState.courseHeaderData.copy(
                            stats = oldState.courseHeaderData.stats.copy(isWishlisted = isWishlisted),
                            isWishlistUpdating = false
                        )
                    )
                },
                onError = emptyOnErrorStub
            )
    }

    /**
     * Analytics
     */
    private fun logCoursePreviewOpenedEvent(course: Course, source: CourseViewSource) {
        if (isCoursePreviewLogged) {
            return
        }
        isCoursePreviewLogged = true
        analytic.report(CoursePreviewScreenOpenedAnalyticEvent(course, source))
        analytic.report(CoursePreviewScreenOpenedAnalyticBatchEvent(course, source))
    }

    private fun logUserCourseAction(userCourseAction: UserCourseAction, source: CourseViewSource) {
        val course = state.safeCast<CourseView.State.CourseLoaded>()
            ?.courseHeaderData
            ?.course
            ?: return

        analytic.report(UserCourseActionEvent(userCourseAction, course, source))
    }

    private fun logWishlistAction(wishlistAction: WishlistAction, source: CourseViewSource) {
        val course = state.safeCast<CourseView.State.CourseLoaded>()
            ?.courseHeaderData
            ?.course
            ?: return

        val event =
            if (wishlistAction == WishlistAction.ADD) {
                CourseWishlistAddedEvent(course, source)
            } else {
                CourseWishlistRemovedEvent(course, source)
            }
        analytic.report(event)
    }

    private fun schedulePurchaseReminder() {
        compositeDisposable += coursePurchaseReminderInteractor
            .savePurchaseNotificationSchedule(courseId)
            .subscribeOn(backgroundScheduler)
            .observeOn(mainScheduler)
            .subscribeBy(
                onError = emptyOnErrorStub
            )
    }
}