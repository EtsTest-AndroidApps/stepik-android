package org.stepik.android.remote.review_instruction

import org.stepik.android.data.review_instruction.source.ReviewInstructionRemoteDataSource
import io.reactivex.Single
import org.stepik.android.domain.review_instruction.model.ReviewInstructionData
import org.stepik.android.remote.review_instruction.service.ReviewInstructionService
import javax.inject.Inject

class ReviewInstructionRemoteDataSourceImpl
@Inject
constructor(
    private val reviewInstructionService: ReviewInstructionService
) : ReviewInstructionRemoteDataSource {
    override fun getReviewInstruction(id: Long): Single<ReviewInstructionData> =
        reviewInstructionService
            .getReviewInstructions(listOf(id))
            .map { ReviewInstructionData(it.instructions.first(), it.rubrics) }
}