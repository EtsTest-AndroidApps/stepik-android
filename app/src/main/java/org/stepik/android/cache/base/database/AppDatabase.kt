package org.stepik.android.cache.base.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.stepik.android.cache.base.mapper.CollectionConverter
import org.stepik.android.cache.base.mapper.DateConverter
import org.stepik.android.cache.review_instruction.dao.ReviewInstructionDao
import org.stepik.android.cache.review_instruction.mapper.ReviewStrategyTypeConverter
import org.stepik.android.cache.review_session.dao.ReviewSessionDao
import org.stepik.android.domain.review_instruction.model.ReviewInstruction
import org.stepik.android.domain.review_session.model.ReviewSession

@Database(
    entities = [
        ReviewInstruction::class,
        ReviewSession::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = false
)
@TypeConverters(
    CollectionConverter::class,
    DateConverter::class,

    ReviewStrategyTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 60
        const val NAME = "stepic_database.db"
    }

    abstract fun reviewInstructionDao(): ReviewInstructionDao
    abstract fun reviewSessionDao(): ReviewSessionDao
}