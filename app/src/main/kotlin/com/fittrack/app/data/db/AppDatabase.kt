package com.fittrack.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ActivityEntity::class, RoutePointEntity::class], version = 1, exportSchema = false)
@TypeConverters(ActivityTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    
    // MIGRATION_1_2 stub for future use
    /*
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration code
            }
        }
    }
    */
}
