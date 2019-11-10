package org.stepic.droid.web;

import org.jetbrains.annotations.Nullable;
import org.stepik.android.remote.assignment.model.AssignmentResponse;
import org.stepik.android.remote.auth.model.StepikProfileResponse;
import org.stepik.android.remote.certificate.model.CertificateResponse;
import org.stepik.android.remote.course.model.CourseResponse;
import org.stepik.android.remote.course.model.CourseReviewSummaryResponse;
import org.stepik.android.remote.course.model.EnrollmentRequest;
import org.stepik.android.remote.discussion_proxy.model.DiscussionProxyResponse;
import org.stepik.android.remote.discussion_thread.model.DiscussionThreadResponse;
import org.stepik.android.remote.email_address.model.EmailAddressResponse;
import org.stepik.android.remote.progress.model.ProgressResponse;
import org.stepik.android.remote.unit.model.UnitResponse;
import org.stepik.android.remote.user.model.UserResponse;
import org.stepik.android.remote.user_activity.model.UserActivityResponse;
import org.stepik.android.remote.view_assignment.model.ViewAssignmentRequest;
import org.stepik.android.remote.vote.model.VoteRequest;
import org.stepik.android.remote.vote.model.VoteResponse;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StepicRestLoggedService {
    @POST("api/enrollments")
    Completable joinCourse(@Body EnrollmentRequest enrollmentCourse);

    @DELETE("api/enrollments/{id}")
    Completable dropCourse(@Path("id") long courseId);

    @GET("api/users")
    Call<UserResponse> getUsers(@Query("ids[]") long[] userIds);

    @GET("api/users")
    Single<UserResponse> getUsersRx(@Query("ids[]") long[] userIds);

    @GET("api/stepics/1")
    Call<StepikProfileResponse> getUserProfile();

    @GET("api/user-courses")
    Single<UserCoursesResponse> getUserCourses(@Query("page") int page);

    @GET("api/courses?exclude_ended=true&is_public=true&order=-activity")
    Single<CourseResponse> getPopularCourses(@Query("page") int page, @Query("language") String language);

    @GET("api/units")
    Call<UnitResponse> getUnits(
            @Query("ids[]") List<Long> units
    );

    @GET("api/units")
    Single<UnitResponse> getUnits(
            @Query("course") final long courseId,
            @Query("lesson") final long lessonId
    );

    @GET("api/progresses")
    Single<ProgressResponse> getProgressesReactive(@Query("ids[]") String[] progresses);

    @GET("api/assignments")
    Single<AssignmentResponse> getAssignments(@Query("ids[]") long[] assignmentsIds);


    @Headers("Content-Type:application/json")
    @POST("api/views")
    Completable postViewedReactive(@Body ViewAssignmentRequest stepAssignment);

    @GET("api/search-results?is_popular=true&is_public=true&type=course")
    Call<SearchResultResponse> getSearchResults(
            @Query("page") int page,
            @Query(value = "query", encoded = true) String encodedQuery,
            @Query("language") String lang
    );

    @GET("api/queries")
    Single<QueriesResponse> getSearchQueries(@Query("query") String query);

    @GET("api/courses")
    Call<CourseResponse> getCourses(@Query("page") int page, @Query("ids[]") long[] courseIds);

    @GET("api/courses")
    Single<CourseResponse> getCoursesReactive(@Query("page") int page, @Query("ids[]") long[] courseIds);

    @GET("api/courses")
    Single<CourseResponse> getCoursesReactive(@Query("ids[]") long[] courseIds);

    @GET("api/email-addresses")
    Call<EmailAddressResponse> getEmailAddresses(@Query("ids[]") long[] ids);

    @GET("api/courses")
    Call<CourseResponse> getCourses(@Query("ids[]") long[] courseIds);

    @PUT("api/notifications/{id}")
    Call<Void> putNotification(@Path("id") long notificationId, @Body NotificationRequest notificationRequest);

    @PUT("api/notifications/{id}")
    Completable putNotificationReactive(@Path("id") long notificationId, @Body NotificationRequest notificationRequest);

    @GET("api/discussion-proxies")
    Single<DiscussionProxyResponse> getDiscussionProxies(@Query("ids[]") String[] ids);

    @GET("api/discussion-threads")
    Single<DiscussionThreadResponse> getDiscussionThreads(@Query("ids[]") String[] ids);

    @PUT("api/votes/{id}")
    Single<VoteResponse> saveVote(@Path("id") String voteId, @Body VoteRequest voteRequest);

    @GET("api/certificates")
    Single<CertificateResponse> getCertificates(@Query("user") long userId, @Query("page") int page);

    @GET("api/notifications")
    Call<NotificationResponse> getNotifications(@Query("page") int page, @Nullable @Query("type") String type);

    @FormUrlEncoded
    @POST("api/notifications/mark-as-read")
    Call<Void> markAsRead(@Nullable @Field(value = "type", encoded = true) String notificationType);

    @GET("api/notification-statuses")
    Single<NotificationStatusesResponse> getNotificationStatuses();

    @GET("api/user-activities/{userId}")
    Call<UserActivityResponse> getUserActivities(@Path("userId") long userId);

    @GET("api/user-activities/{userId}")
    Single<UserActivityResponse> getUserActivitiesReactive(@Path("userId") long userId);

    @GET("api/course-lists?platform=mobile")
    Single<CourseCollectionsResponse> getCourseLists(@Query("language") String language);

    @GET("api/course-review-summaries")
    Single<CourseReviewSummaryResponse> getCourseReviews(@Query("ids[]") long[] reviewSummaryIds);

    @GET("api/tags?is_featured=true")
    Single<TagResponse> getFeaturedTags();

    @GET("api/search-results?is_popular=true&is_public=true&type=course")
    Single<SearchResultResponse> getSearchResultsOfTag(@Query("page") int page, @Query("tag") int id, @Query("language") String lang);


}
