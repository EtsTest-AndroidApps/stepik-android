package org.stepic.droid.storage.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MigrationFrom61To62 : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `CatalogBlockItem` (`id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `language` TEXT NOT NULL, `appearance` TEXT NOT NULL, `isTitleVisible` INTEGER NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}