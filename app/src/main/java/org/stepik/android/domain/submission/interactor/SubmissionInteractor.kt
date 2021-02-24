package org.stepik.android.domain.submission.interactor

import io.reactivex.Single
import org.stepic.droid.preferences.UserPreferences
import org.stepic.droid.util.PagedList
import org.stepic.droid.util.mapNotNullPaged
import ru.nobird.android.core.model.mapToLongArray
import org.stepik.android.domain.attempt.repository.AttemptRepository
import org.stepik.android.domain.filter.model.SubmissionsFilterQuery
import org.stepik.android.domain.submission.repository.SubmissionRepository
import org.stepik.android.model.Submission
import org.stepik.android.model.attempts.Attempt
import org.stepik.android.domain.submission.model.SubmissionItem
import org.stepik.android.domain.user.repository.UserRepository
import org.stepik.android.model.user.User
import javax.inject.Inject

class SubmissionInteractor
@Inject
constructor(
    private val submissionRepository: SubmissionRepository,
    private val attemptRepository: AttemptRepository,
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) {
    fun getSubmissionItems(stepId: Long, isTeacher: Boolean, submissionsFilterQuery: SubmissionsFilterQuery, page: Int = 1): Single<PagedList<SubmissionItem.Data>> =
        submissionRepository
            .getSubmissionsForStep(
                stepId,
                submissionsFilterQuery.copy(user = if (isTeacher) null else userPreferences.userId),
                page
            )
            .flatMap { submissions ->
                resolveSubmissionItems(submissions)
            }

    private fun resolveSubmissionItems(submissions: PagedList<Submission>): Single<PagedList<SubmissionItem.Data>> {
        val attemptIds = submissions.mapToLongArray(Submission::attempt)

        return attemptRepository
            .getAttempts(*attemptIds)
            .flatMap { attempts ->
                resolveAttemptsAndUsers(submissions, attempts)
            }
    }

    private fun resolveAttemptsAndUsers(submissions: PagedList<Submission>, attempts: List<Attempt>): Single<PagedList<SubmissionItem.Data>> =
        userRepository
            .getUsers(attempts.map(Attempt::user))
            .map { users ->
                mapToSubmissionItems(submissions, attempts, users)
            }

    private fun mapToSubmissionItems(submissions: PagedList<Submission>, attempts: List<Attempt>, users: List<User>): PagedList<SubmissionItem.Data> =
        submissions
            .mapNotNullPaged { submission ->
                val attempt = attempts
                    .find { it.id == submission.attempt }
                    ?: return@mapNotNullPaged null

                val user = users
                    .find { it.id == attempt.user }
                    ?: return@mapNotNullPaged null

                SubmissionItem.Data(submission, attempt, user)
            }
}