package org.stepik.android.cache.user_courses.dao

import android.content.ContentValues
import android.database.Cursor
import org.stepic.droid.storage.dao.DaoBase
import org.stepic.droid.storage.operations.DatabaseOperations
import org.stepic.droid.util.getBoolean
import org.stepic.droid.util.getDate
import org.stepic.droid.util.getLong
import org.stepik.android.cache.user_courses.structure.DbStructureUserCourse
import org.stepik.android.domain.user_courses.model.UserCourse
import javax.inject.Inject

class UserCourseDaoImpl
@Inject
constructor(
    databaseOperations: DatabaseOperations
) : DaoBase<UserCourse>(databaseOperations), UserCourseDao {
    override fun getDbName(): String =
        DbStructureUserCourse.TABLE_NAME

    override fun getDefaultPrimaryColumn(): String =
        DbStructureUserCourse.Columns.COURSE

    override fun getDefaultPrimaryValue(persistentObject: UserCourse): String =
        persistentObject.course.toString()

    override fun getContentValues(userCourse: UserCourse): ContentValues {
        val values = ContentValues()

        values.put(DbStructureUserCourse.Columns.ID, userCourse.id)
        values.put(DbStructureUserCourse.Columns.USER, userCourse.user)
        values.put(DbStructureUserCourse.Columns.COURSE, userCourse.course)
        values.put(DbStructureUserCourse.Columns.IS_FAVORITE, userCourse.isFavorite)
        values.put(DbStructureUserCourse.Columns.IS_PINNED, userCourse.isPinned)
        values.put(DbStructureUserCourse.Columns.IS_ARCHIVED, userCourse.isArchived)
        values.put(DbStructureUserCourse.Columns.LAST_VIEWED, userCourse.lastViewed?.time ?: -1)

        return values
    }

    override fun parsePersistentObject(cursor: Cursor): UserCourse =
        UserCourse(
            id = cursor.getLong(DbStructureUserCourse.Columns.ID),
            user = cursor.getLong(DbStructureUserCourse.Columns.USER),
            course = cursor.getLong(DbStructureUserCourse.Columns.COURSE),
            isFavorite = cursor.getBoolean(DbStructureUserCourse.Columns.IS_FAVORITE),
            isPinned = cursor.getBoolean(DbStructureUserCourse.Columns.IS_PINNED),
            isArchived = cursor.getBoolean(DbStructureUserCourse.Columns.IS_ARCHIVED),
            lastViewed = cursor.getDate(DbStructureUserCourse.Columns.LAST_VIEWED)
        )

    override fun getAllUserCourses(whereArgs: Map<String, String>): List<UserCourse> {
        val (query, where) = if (whereArgs.isEmpty()) {
            "SELECT * FROM $dbName " to ""
        } else {
            "SELECT * FROM $dbName WHERE " to whereArgs.keys.joinToString(" = ? AND ", "", "", -1, "", null) + " = ?"
        }

        val orderBy = "ORDER BY ${DbStructureUserCourse.Columns.LAST_VIEWED} DESC"
        return getAllWithQuery(query + where + orderBy, whereArgs.values.toTypedArray())
    }
}