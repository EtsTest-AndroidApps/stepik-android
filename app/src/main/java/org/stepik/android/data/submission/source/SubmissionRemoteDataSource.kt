package org.stepik.android.data.submission.source

import io.reactivex.Single
import org.stepic.droid.util.PagedList
import org.stepik.android.model.Submission

interface SubmissionRemoteDataSource {
    fun createSubmission(submission: Submission): Single<Submission>
    fun getSubmissionsForAttempt(attemptId: Long): Single<List<Submission>>
    fun getSubmissionsForStep(stepId: Long, userId: Long?, status: Submission.Status?, searchQuery: String?, page: Int): Single<PagedList<Submission>>
}