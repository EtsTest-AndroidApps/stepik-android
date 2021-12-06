package org.stepik.android.domain.course_purchase.interactor

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import okhttp3.ResponseBody
import org.stepic.droid.preferences.SharedPreferenceHelper
import org.stepik.android.domain.base.DataSourceType
import org.stepik.android.domain.billing.repository.BillingRepository
import org.stepik.android.domain.course.interactor.CourseBillingInteractor
import org.stepik.android.domain.course.model.CoursePurchasePayload
import org.stepik.android.domain.course.repository.CourseRepository
import org.stepik.android.domain.course_payments.exception.CourseAlreadyOwnedException
import org.stepik.android.domain.course_payments.exception.CoursePurchaseVerificationException
import org.stepik.android.domain.course_payments.model.CoursePayment
import org.stepik.android.domain.course_payments.model.PromoCodeSku
import org.stepik.android.domain.course_payments.repository.CoursePaymentsRepository
import org.stepik.android.domain.lesson.repository.LessonRepository
import org.stepik.android.domain.mobile_tiers.repository.LightSkuRepository
import org.stepik.android.domain.mobile_tiers.repository.MobileTiersRepository
import org.stepik.android.domain.user_courses.interactor.UserCoursesInteractor
import org.stepik.android.model.Course
import org.stepik.android.remote.mobile_tiers.model.MobileTierCalculation
import org.stepik.android.view.injection.course.EnrollmentCourseUpdates
import retrofit2.HttpException
import retrofit2.Response
import java.net.HttpURLConnection
import javax.inject.Inject

class CoursePurchaseInteractor
@Inject
constructor(
    private val billingRepository: BillingRepository,
    private val mobileTiersRepository: MobileTiersRepository,
    private val lightSkuRepository: LightSkuRepository,
    private val userCoursesInteractor: UserCoursesInteractor,
    private val coursePaymentsRepository: CoursePaymentsRepository,

    private val sharedPreferenceHelper: SharedPreferenceHelper,

    private val courseRepository: CourseRepository,
    private val lessonRepository: LessonRepository,

    @EnrollmentCourseUpdates
    private val enrollmentSubject: PublishSubject<Course>,
    private val gson: Gson
) {
    companion object {
        private val UNAUTHORIZED_EXCEPTION_STUB =
            HttpException(Response.error<Nothing>(HttpURLConnection.HTTP_UNAUTHORIZED, ResponseBody.create(null, "")))
    }

    fun checkPromoCodeValidity(courseId: Long, promoCodeName: String): Single<PromoCodeSku> =
        mobileTiersRepository
            .calculateMobileTier(MobileTierCalculation(course = courseId, promo = promoCodeName), dataSourceType = DataSourceType.REMOTE)
            .flatMapSingle { mobileTier ->
                if (mobileTier.promoTier == null) {
                    Single.just(PromoCodeSku.EMPTY)
                } else {
                    lightSkuRepository
                        .getLightInventory(BillingClient.SkuType.INAPP, listOf(mobileTier.promoTier), dataSourceType = DataSourceType.REMOTE)
                        .map { lightSku -> PromoCodeSku(promoCodeName, lightSku.firstOrNull()) }
                }
            }

    fun launchPurchaseFlow(courseId: Long, skuId: String): Single<Pair<String, SkuDetails>> =
        coursePaymentsRepository
            .getCoursePaymentsByCourseId(courseId, CoursePayment.Status.SUCCESS, sourceType = DataSourceType.REMOTE)
            .flatMap { payments ->
                if (payments.isEmpty()) {
                    getSkuDetails(courseId, skuId)
                } else {
                    Single.error(CourseAlreadyOwnedException(courseId))
                }
            }

    fun completePurchase(courseId: Long, sku: SkuDetails, purchase: Purchase): Completable =
        coursePaymentsRepository
            .createCoursePayment(courseId, sku, purchase)
            .flatMapCompletable { payment ->
                if (payment.status == CoursePayment.Status.SUCCESS) {
                    Completable.complete()
                } else {
                    Completable.error(CoursePurchaseVerificationException())
                }
            }
            .andThen(updateCourseAfterEnrollment(courseId))
            .andThen(billingRepository.consumePurchase(purchase))

    private fun getSkuDetails(courseId: Long, skuId: String): Single<Pair<String, SkuDetails>> =
        getCurrentProfileId()
            .flatMap { profileId ->
                val payload = CoursePurchasePayload(profileId, courseId).hashCode().toString()
                billingRepository
                    .getInventory(BillingClient.SkuType.INAPP, skuId)
                    .toSingle()
                    .map { skuDetails -> payload to skuDetails }
            }


    private fun updateCourseAfterEnrollment(courseId: Long): Completable =
        userCoursesInteractor.addUserCourse(courseId)
            .andThen(lessonRepository.removeCachedLessons(courseId))
            .andThen(courseRepository.getCourse(courseId, sourceType = DataSourceType.REMOTE, allowFallback = false).toSingle())
            .doOnSuccess(enrollmentSubject::onNext) // notify everyone about changes
            .ignoreElement()

    private fun getCurrentProfileId(): Single<Long> =
        Single.fromCallable {
            sharedPreferenceHelper
                .profile
                ?.id
                ?: throw UNAUTHORIZED_EXCEPTION_STUB
        }
}